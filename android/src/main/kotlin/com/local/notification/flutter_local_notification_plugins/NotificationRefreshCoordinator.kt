package com.local.notification.flutter_local_notification_plugins

import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/** Coordinates short-lived notification refreshes without re-entering the full display flow. */
object NotificationRefreshCoordinator {
    private const val TAG = "NotificationRefresh"
    private const val RECEIVER_COMPLETION_GRACE_MILLIS = 5_000L
    private const val ACTION_REFRESH =
        "com.local.notification.flutter_local_notification_plugins.REFRESH_NOTIFICATION"
    private const val EXTRA_SESSION_TOKEN = "notification_refresh_session_token"
    private const val EXTRA_REFRESH_INDEX = "notification_refresh_index"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lock = Any()

    private data class RefreshSession(
        val token: String,
        val tag: String?,
        val id: Int,
        val notification: Notification,
        val payload: String?,
        val refreshCount: Int,
        val refreshIntervalMillis: Long,
        val expireAtElapsedRealtime: Long?,
    )

    private data class Eligibility(
        val allowed: Boolean,
        val reason: String,
    )

    private var currentSession: RefreshSession? = null
    private var refreshJob: Job? = null
    private val sessionGate = NotificationRefreshSessionGate()

    fun schedule(
        context: Context,
        tag: String?,
        id: Int,
        notification: Notification,
        payload: String?,
        expireAtElapsedRealtime: Long? = null,
    ) {
        val appContext = context.applicationContext
        val config = NotificationConfigStore.read(appContext)
        val refreshCount = (config.pushNotifyTotalCount - 1).coerceAtLeast(0)
        if (refreshCount == 0) {
            cancel("refresh_disabled")
            return
        }
        val eligibility = refreshEligibility(appContext)
        if (!eligibility.allowed) {
            cancel("schedule_${eligibility.reason}")
            Log.d(
                TAG,
                "schedule skipped reason=${eligibility.reason} tag=$tag id=$id payload=$payload",
            )
            return
        }

        val session =
            RefreshSession(
                token = UUID.randomUUID().toString(),
                tag = tag,
                id = id,
                notification = notification,
                payload = payload,
                refreshCount = refreshCount,
                refreshIntervalMillis = config.pushRefreshIntervalMillis,
                expireAtElapsedRealtime = expireAtElapsedRealtime,
            )
        synchronized(lock) {
            refreshJob?.cancel()
            currentSession = session
            sessionGate.start(session.token, session.refreshCount)
            refreshJob =
                scope.launch {
                    repeat(session.refreshCount) { offset ->
                        delay(session.refreshIntervalMillis)
                        val refreshIndex = offset + 1
                        if (!isCurrentSession(session.token)) {
                            return@launch
                        }
                        val currentEligibility = refreshEligibility(appContext)
                        if (!currentEligibility.allowed) {
                            cancel("before_broadcast_${currentEligibility.reason}")
                            return@launch
                        }
                        appContext.sendBroadcast(
                            Intent(appContext, NotificationRefreshReceiver::class.java).apply {
                                action = ACTION_REFRESH
                                setPackage(appContext.packageName)
                                putExtra(EXTRA_SESSION_TOKEN, session.token)
                                putExtra(EXTRA_REFRESH_INDEX, refreshIndex)
                            },
                        )
                    }
                    delay(RECEIVER_COMPLETION_GRACE_MILLIS)
                    finishSession(session.token, "receiver_timeout")
                }
        }
        Log.d(
            TAG,
            "schedule started tag=$tag id=$id payload=$payload refreshCount=${session.refreshCount} totalNotifyCount=${session.refreshCount + 1}",
        )
    }

    fun handleRefreshIntent(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action != ACTION_REFRESH) return
        val token = intent.getStringExtra(EXTRA_SESSION_TOKEN) ?: return
        val refreshIndex = intent.getIntExtra(EXTRA_REFRESH_INDEX, 0)
        val appContext = context.applicationContext
        val eligibility = refreshEligibility(appContext)
        if (!eligibility.allowed) {
            cancel("receiver_${eligibility.reason}")
            return
        }

        val session =
            synchronized(lock) {
                val current = currentSession
                if (
                    current == null || refreshIndex !in 1..current.refreshCount ||
                        !sessionGate.claim(token, refreshIndex)
                ) {
                    null
                } else {
                    current
                }
            } ?: return

        try {
            val notificationManager = NotificationManagerCompat.from(appContext)
            val remainingTimeoutMillis =
                session.expireAtElapsedRealtime?.minus(SystemClock.elapsedRealtime())
            if (remainingTimeoutMillis != null && remainingTimeoutMillis <= 0L) {
                if (session.tag.isNullOrBlank()) {
                    notificationManager.cancel(session.id)
                } else {
                    notificationManager.cancel(session.tag, session.id)
                }
                finishSession(token, "notification_expired")
                Log.d(
                    TAG,
                    "notify skipped expired tag=${session.tag} id=${session.id} payload=${session.payload}",
                )
                return
            }
            val notificationToRefresh =
                if (remainingTimeoutMillis != null) {
                    Notification.Builder
                        .recoverBuilder(appContext, session.notification)
                        .setTimeoutAfter(remainingTimeoutMillis)
                        .build()
                } else {
                    session.notification
                }
            if (session.tag.isNullOrBlank()) {
                notificationManager.notify(session.id, notificationToRefresh)
            } else {
                notificationManager.notify(session.tag, session.id, notificationToRefresh)
            }
            Log.d(
                TAG,
                "notify refreshIndex=$refreshIndex tag=${session.tag} id=${session.id} payload=${session.payload}",
            )
            if (refreshIndex == session.refreshCount) {
                finishSession(token, "completed")
            }
        } catch (error: Exception) {
            Log.e(
                TAG,
                "notify failed refreshIndex=$refreshIndex tag=${session.tag} id=${session.id}",
                error,
            )
            cancel("notify_failed")
        }
    }

    fun cancel(reason: String) {
        val cancelled =
            synchronized(lock) {
                val hadSession = currentSession != null || refreshJob?.isActive == true
                currentSession = null
                sessionGate.cancel()
                refreshJob?.cancel()
                refreshJob = null
                hadSession
            }
        if (cancelled) {
            Log.d(TAG, "cancel reason=$reason")
        }
    }

    private fun finishSession(
        token: String,
        reason: String,
    ) {
        val finished =
            synchronized(lock) {
                if (currentSession?.token != token || !sessionGate.finish(token)) {
                    false
                } else {
                    currentSession = null
                    refreshJob?.cancel()
                    refreshJob = null
                    true
                }
            }
        if (finished) {
            Log.d(TAG, "finish reason=$reason")
        }
    }

    private fun isCurrentSession(token: String): Boolean =
        synchronized(lock) { currentSession?.token == token && sessionGate.isCurrent(token) }

    private fun refreshEligibility(context: Context): Eligibility {
        if (FlutterLocalNotificationPluginsPlugin.isHostActivityInForeground()) {
            return Eligibility(false, "app_foreground")
        }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager?.isInteractive != true) {
            return Eligibility(false, "screen_off")
        }
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager?.isKeyguardLocked != false) {
            return Eligibility(false, "device_locked")
        }
        if (!FlutterLocalNotificationPluginsPlugin.canPostNotifications(context)) {
            return Eligibility(false, "notification_permission_off")
        }
        return Eligibility(true, "allowed")
    }
}
