package com.local.notification.flutter_local_notification_plugins

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log

object KeepAliveServiceState {
    enum class State {
        IDLE,
        STARTING,
        STARTED,
        STOPPING,
    }

    private const val TAG = "KeepAliveServiceState"
    private const val PREFS_NAME = "flutter_local_notification_plugins"
    private const val KEY_STARTED_AT = "keep_alive_service_started_at"
    private const val KEY_HEARTBEAT_AT = "keep_alive_service_heartbeat_at"
    private const val KEY_LAST_START_REASON = "keep_alive_service_last_start_reason"
    private const val HEARTBEAT_STALE_MILLIS = 10L * 60L * 1000L

    @Volatile
    var state: State = State.IDLE
        private set

    @Volatile
    private var startingAtElapsed: Long = 0L

    fun markStarting(context: Context, reason: String): Boolean {
        synchronized(this) {
            if (state == State.STARTED) return false
            if (state == State.STARTING &&
                SystemClock.elapsedRealtime() - startingAtElapsed < 15_000L
            ) {
                return false
            }
            state = State.STARTING
            startingAtElapsed = SystemClock.elapsedRealtime()
            prefs(context).edit().putString(KEY_LAST_START_REASON, reason).apply()
            Log.d(TAG, "state=STARTING reason=$reason")
            return true
        }
    }

    fun markStarted(context: Context, reason: String) {
        state = State.STARTED
        val now = System.currentTimeMillis()
        prefs(context).edit()
            .putLong(KEY_STARTED_AT, now)
            .putLong(KEY_HEARTBEAT_AT, now)
            .putString(KEY_LAST_START_REASON, reason)
            .apply()
        Log.d(TAG, "state=STARTED reason=$reason")
    }

    fun heartbeat(context: Context) {
        if (state != State.STARTED) return
        prefs(context).edit().putLong(KEY_HEARTBEAT_AT, System.currentTimeMillis()).apply()
    }

    fun markStopping(reason: String) {
        state = State.STOPPING
        Log.d(TAG, "state=STOPPING reason=$reason")
    }

    fun markIdle(context: Context, reason: String) {
        state = State.IDLE
        startingAtElapsed = 0L
        prefs(context).edit().remove(KEY_HEARTBEAT_AT).apply()
        Log.d(TAG, "state=IDLE reason=$reason")
    }

    fun isHealthy(context: Context): Boolean {
        if (state != State.STARTED) return false
        val heartbeatAt = prefs(context).getLong(KEY_HEARTBEAT_AT, 0L)
        if (heartbeatAt <= 0L || System.currentTimeMillis() - heartbeatAt > HEARTBEAT_STALE_MILLIS) {
            return false
        }
        return hasForegroundNotification(context)
    }

    fun hasForegroundNotification(context: Context): Boolean {
        return try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.activeNotifications.any {
                it.id == KeepAliveNotificationHelper.SHORTCUT_NOTIFICATION_ID &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                        it.notification.channelId == KeepAliveNotificationHelper.SHORTCUT_CHANNEL_ID)
            }
        } catch (e: Exception) {
            Log.d(TAG, "hasForegroundNotification failed error=${e.message}")
            false
        }
    }

    fun describeForegroundNotification(context: Context): String {
        return try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val active = manager.activeNotifications
            val target = active.firstOrNull { it.id == KeepAliveNotificationHelper.SHORTCUT_NOTIFICATION_ID }
            val channelDescription =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = manager.getNotificationChannel(KeepAliveNotificationHelper.SHORTCUT_CHANNEL_ID)
                    "channel=${target?.notification?.channelId} importance=${channel?.importance}"
                } else {
                    "channel=pre_o"
                }
            "active=${target != null} $channelDescription activeCount=${active.size} state=$state"
        } catch (e: Exception) {
            "inspect_failed=${e.message} state=$state"
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
