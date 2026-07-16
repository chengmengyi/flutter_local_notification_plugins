package com.local.notification.flutter_local_notification_plugins

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class KeepAliveForegroundService : Service() {
    companion object {
        private const val TAG = "KeepAliveForegroundSvc"
        private const val HEARTBEAT_INTERVAL_MILLIS = 2L * 60L * 1000L
        private val TASK_REMOVED_CHECK_DELAYS = longArrayOf(5_000L, 30_000L, 2L * 60L * 1000L)
    }

    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private var recoveryScheduled = false
    private val heartbeatRunnable =
        object : Runnable {
            override fun run() {
                verifyAndRefreshForegroundNotification("heartbeat")
                KeepAliveServiceState.heartbeat(applicationContext)
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MILLIS)
            }
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val reason = intent?.getStringExtra("restart_reason") ?: "service_start"
        try {
            InProcessTimerManager.start(applicationContext)
            if (KeepAliveServiceState.isHealthy(applicationContext)) {
                KeepAliveServiceState.heartbeat(applicationContext)
                return START_STICKY
            }
            if (KeepAliveServiceState.state == KeepAliveServiceState.State.IDLE) {
                KeepAliveServiceState.markStarting(applicationContext, reason)
            }
            FlutterLocalNotificationPluginsPlugin.restoreBroadcastReceivers(applicationContext)
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
                KeepAliveServiceState.markStopping("notification_config_empty")
                stopSelf()
                return START_NOT_STICKY
            }
            runServiceStep("schedule_short_monitor") {
                KeepAliveNotificationHelper.scheduleShortMonitorJob(
                    applicationContext,
                    immediate = false,
                )
            }
            runServiceStep("schedule_long_patrol") {
                KeepAliveNotificationHelper.scheduleLongPatrolJob(applicationContext)
            }
            runServiceStep("schedule_work_manager") {
                KeepAliveNotificationHelper.scheduleKeepAliveWork(applicationContext)
            }
            if (!ignoreNotificationPermission &&
                !FlutterLocalNotificationPluginsPlugin.canPostNotifications(applicationContext)
            ) {
                Log.d(TAG, "onStartCommand skipped foreground, notification permission off")
                KeepAliveServiceState.markStopping("notification_permission_off")
                stopSelf()
                return START_NOT_STICKY
            }
            try {
                promoteToForeground(notification)
                KeepAliveServiceState.markStarted(applicationContext, reason)
                KeepAliveNotificationHelper.resetRecoveryAttempts(applicationContext)
                heartbeatHandler.removeCallbacks(heartbeatRunnable)
                heartbeatHandler.post(heartbeatRunnable)
                GalleryImageObserverHelper.start(applicationContext)
            } catch (e: Exception) {
                KeepAliveServiceState.markIdle(applicationContext, "start_foreground_failed")
                Log.d(TAG, "onStartCommand failed reason=$reason error=${e.message}")
                scheduleRecovery("service_start_failed:$reason")
                stopSelf()
            }
            return START_STICKY
        } catch (e: Exception) {
            KeepAliveServiceState.markIdle(applicationContext, "service_fatal")
            Log.d(TAG, "onStartCommand fatal reason=$reason error=${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            Log.d(TAG, "onTaskRemoved")
            TASK_REMOVED_CHECK_DELAYS.forEach { delayMillis ->
                heartbeatHandler.postDelayed(
                    { verifyAndRefreshForegroundNotification("task_removed_${delayMillis}ms") },
                    delayMillis,
                )
            }
            scheduleRecovery("task_removed")
        } catch (e: Exception) {
            Log.d(TAG, "onTaskRemoved failed error=${e.message}")
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        try {
            Log.d(TAG, "onDestroy")
            val shouldRecover =
                KeepAliveServiceState.state != KeepAliveServiceState.State.STOPPING &&
                    FlutterLocalNotificationPluginsPlugin.canPostNotifications(applicationContext)
            heartbeatHandler.removeCallbacks(heartbeatRunnable)
            KeepAliveServiceState.markIdle(applicationContext, "service_destroyed")
            GalleryImageObserverHelper.stop(applicationContext)
            if (shouldRecover) {
                scheduleRecovery("service_destroyed")
            }
        } catch (e: Exception) {
            Log.d(TAG, "onDestroy failed error=${e.message}")
        }
        super.onDestroy()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.d(
            TAG,
            "onTimeout startId=$startId fgsType=$fgsType state=${KeepAliveServiceState.state}",
        )
        scheduleRecovery("system_timeout")
        KeepAliveServiceState.markStopping("system_timeout")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf(startId)
    }

    private fun scheduleRecovery(reason: String) {
        if (recoveryScheduled) return
        recoveryScheduled = true
        KeepAliveNotificationHelper.scheduleRecoveryRetry(applicationContext, reason)
    }

    private fun verifyAndRefreshForegroundNotification(reason: String) {
        if (KeepAliveServiceState.state != KeepAliveServiceState.State.STARTED) return
        val before = KeepAliveServiceState.describeForegroundNotification(applicationContext)
        val notification =
            KeepAliveNotificationHelper.buildPersistentShortcutNotification(applicationContext)
                ?: return
        try {
            promoteToForeground(notification)
            KeepAliveServiceState.heartbeat(applicationContext)
            val after = KeepAliveServiceState.describeForegroundNotification(applicationContext)
            Log.d(TAG, "verifyAndRefresh reason=$reason before={$before} after={$after}")
        } catch (e: Exception) {
            Log.d(TAG, "verifyAndRefresh failed reason=$reason before={$before} error=${e.message}")
            KeepAliveServiceState.markIdle(applicationContext, "refresh_failed:$reason")
            scheduleRecovery("refresh_failed:$reason")
        }
    }

    private fun promoteToForeground(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                KeepAliveNotificationHelper.SHORTCUT_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(KeepAliveNotificationHelper.SHORTCUT_NOTIFICATION_ID, notification)
        }
    }

    private fun runServiceStep(
        step: String,
        block: () -> Unit,
    ) {
        try {
            block()
        } catch (e: Exception) {
            Log.d(TAG, "onStartCommand step=$step failed error=${e.message}")
        }
    }
}
