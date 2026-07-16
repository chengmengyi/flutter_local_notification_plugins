package com.local.notification.flutter_local_notification_plugins

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class LocalKeepAliveWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : Worker(appContext, workerParams) {
    companion object {
        private const val TAG = "LocalKeepAliveWorker"
    }

    override fun doWork(): Result {
        Log.d(TAG, "doWork start")
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(applicationContext)) {
            Log.d(TAG, "doWork blocked by manufacturer")
            return Result.success()
        }
        return try {
            KeepAliveNotificationHelper.ensureForegroundServiceAlive(
                applicationContext,
                "work_manager",
            )
            LocalNotificationScheduler.reconcile(applicationContext)
            KeepAliveNotificationHelper.scheduleLongPatrolJob(applicationContext)
            KeepAliveNotificationHelper.scheduleShortMonitorJob(applicationContext)
            Log.d(TAG, "doWork end")
            Result.success()
        } catch (e: Exception) {
            Log.d(TAG, "doWork failed error=${e.message}")
            Result.retry()
        }
    }
}
