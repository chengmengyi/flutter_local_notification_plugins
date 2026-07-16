package com.local.notification.flutter_local_notification_plugins

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object FixedTimerAlarmManager {
    const val ACTION_TIMER_ALARM_1 =
        "com.local.notification.flutter_local_notification_plugins.TIMER_ALARM_1"
    const val ACTION_TIMER_ALARM_2 =
        "com.local.notification.flutter_local_notification_plugins.TIMER_ALARM_2"
    const val ACTION_TIMER_ALARM_3 =
        "com.local.notification.flutter_local_notification_plugins.TIMER_ALARM_3"

    private const val TAG = "FixedTimerAlarmManager"
    private const val PREFS_NAME = "flutter_local_notification_plugins"
    private const val KEY_PREFIX = "fixed_timer_alarm_"
    private const val REQUEST_CODE_1 = 431001
    private const val REQUEST_CODE_2 = 431002
    private const val REQUEST_CODE_3 = 431003
    private val lock = Any()

    fun schedule(context: Context, scheduleId: Int, update: Boolean) {
        val config =
            LocalNotificationScheduler.timerWorkConfigs(context)
                .firstOrNull { it.scheduleId == scheduleId } ?: return
        scheduleSlot(context.applicationContext, config, resetTime = update)
    }

    fun restore(context: Context) {
        val appContext = context.applicationContext
        val configs = LocalNotificationScheduler.timerWorkConfigs(appContext)
        val configuredSlots = configs.map { it.slot }.toSet()
        configs.forEach { scheduleSlot(appContext, it, resetTime = false) }
        (1..3).filterNot { it in configuredSlots }.forEach { cancelSlot(appContext, it, clear = true) }
        Log.d(TAG, "restore count=${configs.size}")
    }

    fun handleAlarm(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val slot = slotForAction(intent.action) ?: return
        val config =
            LocalNotificationScheduler.timerWorkConfigs(appContext)
                .firstOrNull { it.slot == slot }
        if (config == null) {
            cancelSlot(appContext, slot, clear = true)
            return
        }
        val source = "fixed_alarm_$slot"
        runCatching {
            val displayed =
                LocalNotificationScheduler.tryDeliverFromTimerWork(
                    appContext,
                    config.scheduleId,
                    source,
                )
            val foregroundRequested =
                KeepAliveNotificationHelper.startOrUpdateForegroundService(appContext, source)
            Log.d(
                TAG,
                "handleAlarm slot=$slot scheduleId=${config.scheduleId} displayed=$displayed foregroundRequested=$foregroundRequested",
            )
        }.onFailure {
            Log.d(TAG, "handleAlarm failed slot=$slot error=${it.message}")
        }
        advanceAndScheduleNext(appContext, config)
    }

    fun cancelAll(context: Context, clear: Boolean) {
        (1..3).forEach { cancelSlot(context.applicationContext, it, clear) }
    }

    private fun scheduleSlot(
        context: Context,
        config: LocalNotificationScheduler.TimerWorkConfig,
        resetTime: Boolean,
    ) {
        val triggerAt = synchronized(lock) {
            val prefs = prefs(context)
            val storedScheduleId = prefs.getInt(key(config.slot, "schedule_id"), 0)
            val storedInterval = prefs.getLong(key(config.slot, "interval"), 0L)
            val storedNext = prefs.getLong(key(config.slot, "next_at"), 0L)
            val unchanged =
                !resetTime &&
                    storedScheduleId == config.scheduleId &&
                    storedInterval == config.intervalMillis &&
                    storedNext > 0L
            val next =
                if (unchanged) {
                    storedNext
                } else {
                    System.currentTimeMillis() + config.intervalMillis
                }
            prefs.edit()
                .putInt(key(config.slot, "schedule_id"), config.scheduleId)
                .putLong(key(config.slot, "interval"), config.intervalMillis)
                .putLong(key(config.slot, "next_at"), next)
                .apply()
            next
        }
        setAlarm(context, config.slot, triggerAt)
        Log.d(
            TAG,
            "schedule slot=${config.slot} scheduleId=${config.scheduleId} triggerAt=$triggerAt resetTime=$resetTime",
        )
    }

    private fun advanceAndScheduleNext(
        context: Context,
        config: LocalNotificationScheduler.TimerWorkConfig,
    ) {
        val next = synchronized(lock) {
            val prefs = prefs(context)
            val now = System.currentTimeMillis()
            var candidate = prefs.getLong(key(config.slot, "next_at"), now) + config.intervalMillis
            while (candidate <= now) candidate += config.intervalMillis
            prefs.edit()
                .putInt(key(config.slot, "schedule_id"), config.scheduleId)
                .putLong(key(config.slot, "interval"), config.intervalMillis)
                .putLong(key(config.slot, "next_at"), candidate)
                .apply()
            candidate
        }
        setAlarm(context, config.slot, next)
    }

    private fun setAlarm(context: Context, slot: Int, triggerAt: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent =
            createPendingIntent(context, slot, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        alarmManager.cancel(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun cancelSlot(context: Context, slot: Int, clear: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val pendingIntent = createPendingIntent(context, slot, PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        if (clear) {
            prefs(context).edit()
                .remove(key(slot, "schedule_id"))
                .remove(key(slot, "interval"))
                .remove(key(slot, "next_at"))
                .apply()
        }
    }

    private fun createPendingIntent(context: Context, slot: Int, flag: Int): PendingIntent? {
        val intent = Intent(context, FixedTimerAlarmReceiver::class.java).apply {
            action = actionForSlot(slot)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeForSlot(slot),
            intent,
            flag or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun slotForAction(action: String?): Int? =
        when (action) {
            ACTION_TIMER_ALARM_1 -> 1
            ACTION_TIMER_ALARM_2 -> 2
            ACTION_TIMER_ALARM_3 -> 3
            else -> null
        }

    private fun actionForSlot(slot: Int) =
        when (slot) {
            1 -> ACTION_TIMER_ALARM_1
            2 -> ACTION_TIMER_ALARM_2
            else -> ACTION_TIMER_ALARM_3
        }

    private fun requestCodeForSlot(slot: Int) =
        when (slot) {
            1 -> REQUEST_CODE_1
            2 -> REQUEST_CODE_2
            else -> REQUEST_CODE_3
        }

    private fun key(slot: Int, suffix: String) = "$KEY_PREFIX${slot}_$suffix"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
