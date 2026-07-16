package com.local.notification.flutter_local_notification_plugins

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object TimerNotificationWorkManager {
    const val INPUT_TIMER_SLOT = "timer_slot"
    const val INPUT_SCHEDULE_ID = "schedule_id"
    private const val TAG = "TimerNotificationWorkMgr"
    private const val MIN_PERIODIC_INTERVAL_MILLIS = 15L * 60L * 1000L
    private const val WORK_NAME_PREFIX = "pdf_flow_timer_notification_work_"

    fun schedule(context: Context, scheduleId: Int, update: Boolean) {
        val config =
            LocalNotificationScheduler.timerWorkConfigs(context)
                .firstOrNull { it.scheduleId == scheduleId } ?: return
        enqueue(context.applicationContext, config, update)
    }

    fun restore(context: Context) {
        val appContext = context.applicationContext
        val configs = LocalNotificationScheduler.timerWorkConfigs(appContext)
        val configuredSlots = configs.map { it.slot }.toSet()
        configs.forEach { enqueue(appContext, it, update = false) }
        (1..3).filterNot { it in configuredSlots }.forEach { slot ->
            WorkManager.getInstance(appContext).cancelUniqueWork(workName(slot))
        }
        Log.d(TAG, "restore count=${configs.size}")
    }

    fun cancelAll(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        (1..3).forEach { workManager.cancelUniqueWork(workName(it)) }
    }

    private fun enqueue(
        context: Context,
        config: LocalNotificationScheduler.TimerWorkConfig,
        update: Boolean,
    ) {
        val interval = config.intervalMillis.coerceAtLeast(MIN_PERIODIC_INTERVAL_MILLIS)
        val request =
            PeriodicWorkRequestBuilder<TimerNotificationWorker>(interval, TimeUnit.MILLISECONDS)
                .setInitialDelay(interval, TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putInt(INPUT_TIMER_SLOT, config.slot)
                        .putInt(INPUT_SCHEDULE_ID, config.scheduleId)
                        .build(),
                )
                .addTag(workName(config.slot))
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName(config.slot),
            if (update) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        Log.d(
            TAG,
            "enqueue slot=${config.slot} scheduleId=${config.scheduleId} interval=$interval update=$update",
        )
    }

    private fun workName(slot: Int) = "$WORK_NAME_PREFIX$slot"
}
