package com.local.notification.flutter_local_notification_plugins

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import kotlin.concurrent.thread

class LocalKeepAliveJobService : JobService() {
    companion object {
        private const val TAG = "LocalKeepAliveJobSvc"
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        if (params == null) {
            return false
        }
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(applicationContext)) {
            Log.d(TAG, "onStartJob blocked")
            return false
        }
        val mode = params.extras?.getString("job_mode") ?: "monitor"
        Log.d(TAG, "onStartJob mode=$mode")
        thread(name = "local-keep-alive-job-$mode") {
            try {
                KeepAliveNotificationHelper.handleJob(applicationContext, mode)
            } catch (e: Exception) {
                Log.d(TAG, "onStartJob failed mode=$mode error=${e.message}")
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d(TAG, "onStopJob")
        return false
    }
}
