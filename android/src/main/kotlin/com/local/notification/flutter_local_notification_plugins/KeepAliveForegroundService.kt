package com.local.notification.flutter_local_notification_plugins

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

class KeepAliveForegroundService : Service() {
    companion object {
        private const val TAG = "KeepAliveForegroundSvc"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val reason = intent?.getStringExtra("restart_reason") ?: "service_start"
        val ignoreNotificationPermission =
            intent?.getBooleanExtra(
                KeepAliveNotificationHelper.EXTRA_IGNORE_NOTIFICATION_PERMISSION,
                false,
            ) == true
        Log.d(TAG, "onStartCommand reason=$reason")
        val notification =
            KeepAliveNotificationHelper.buildPersistentShortcutNotification(applicationContext)
        if (notification == null) {
            Log.d(TAG, "onStartCommand skipped, notification config empty")
            stopSelf()
            return START_NOT_STICKY
        }
        KeepAliveNotificationHelper.scheduleShortMonitorJob(
            applicationContext,
            immediate = false,
        )
        KeepAliveNotificationHelper.scheduleLongPatrolJob(applicationContext)
        KeepAliveNotificationHelper.scheduleKeepAliveWork(applicationContext)
        if (!ignoreNotificationPermission &&
            !FlutterLocalNotificationPluginsPlugin.canPostNotifications(applicationContext)
        ) {
            Log.d(TAG, "onStartCommand skipped foreground, notification permission off")
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    10004,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(10004, notification)
            }
            GalleryImageObserverHelper.start(applicationContext)
        } catch (e: Exception) {
            Log.d(TAG, "onStartCommand failed reason=$reason error=${e.message}")
            KeepAliveNotificationHelper.scheduleRestartFallback(
                applicationContext,
                "service_start_failed:$reason",
            )
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved")
        KeepAliveNotificationHelper.scheduleRestartFallback(
            applicationContext,
            "task_removed",
        )
        KeepAliveNotificationHelper.scheduleShortMonitorJob(
            applicationContext,
            immediate = true,
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        GalleryImageObserverHelper.stop(applicationContext)
        KeepAliveNotificationHelper.scheduleRestartFallback(
            applicationContext,
            "service_destroyed",
        )
        KeepAliveNotificationHelper.scheduleShortMonitorJob(
            applicationContext,
            immediate = true,
        )
        super.onDestroy()
    }
}
