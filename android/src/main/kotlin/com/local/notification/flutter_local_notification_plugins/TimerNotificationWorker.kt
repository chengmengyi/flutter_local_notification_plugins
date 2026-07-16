package com.local.notification.flutter_local_notification_plugins

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class TimerNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val slot = inputData.getInt(TimerNotificationWorkManager.INPUT_TIMER_SLOT, 0)
        val scheduleId = inputData.getInt(TimerNotificationWorkManager.INPUT_SCHEDULE_ID, 0)
        val source = "timer_work_$slot"
        runCatching {
            val displayed =
                LocalNotificationScheduler.tryDeliverFromTimerWork(
                    applicationContext,
                    scheduleId,
                    source,
                )
            val foregroundRequested =
                KeepAliveNotificationHelper.startOrUpdateForegroundService(
                    applicationContext,
                    source,
                )
            Log.d(
                TAG,
                "doWork slot=$slot scheduleId=$scheduleId displayed=$displayed foregroundRequested=$foregroundRequested",
            )
        }.onFailure {
            Log.d(TAG, "doWork failed slot=$slot scheduleId=$scheduleId error=${it.message}")
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "TimerNotificationWorker"
    }
}
