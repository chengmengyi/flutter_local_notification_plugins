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
    private var foregroundPromoted = false
    private val heartbeatRunnable =
        object : Runnable {
            override fun run() {
                try {
                    verifyAndRefreshForegroundNotification("heartbeat")
                    KeepAliveServiceState.heartbeat(applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "heartbeat failed", e)
                    runCatching { scheduleRecovery("heartbeat_exception") }
                } finally {
                    runCatching { heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MILLIS) }
                        .onFailure { Log.e(TAG, "heartbeat reschedule failed", it) }
                }
            }
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val reason = intent?.getStringExtra("restart_reason") ?: "service_start"
        val allowStartFailureRecovery =
            intent?.getBooleanExtra(
                KeepAliveNotificationHelper.EXTRA_ALLOW_START_FAILURE_RECOVERY,
                true,
            ) ?: true
        try {
            if (foregroundPromoted && KeepAliveServiceState.isHealthy(applicationContext)) {
                KeepAliveServiceState.heartbeat(applicationContext)
                return START_STICKY
            }
            // Reaching a non-healthy service start means the process has been pulled alive.
            // Session reporting must not depend on notification config or permission checks below.
            SessionBackgroundReporter.onKeepAliveStarted(applicationContext)
            if (KeepAliveServiceState.state == KeepAliveServiceState.State.STARTED) {
                KeepAliveServiceState.markIdle(applicationContext, "health_check_failed:$reason")
            }
            if (KeepAliveServiceState.state == KeepAliveServiceState.State.IDLE) {
                KeepAliveServiceState.markStarting(applicationContext, reason)
            }
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
                logFcmStartFailureIfNeeded(reason, "notification config empty")
                KeepAliveServiceState.markStopping("notification_config_empty")
                stopSelf()
                return START_NOT_STICKY
            }
            if (!ignoreNotificationPermission &&
                !FlutterLocalNotificationPluginsPlugin.canPostNotifications(applicationContext)
            ) {
                Log.d(TAG, "onStartCommand skipped foreground, notification permission off")
                logFcmStartFailureIfNeeded(reason, "notification permission off")
                KeepAliveServiceState.markStopping("notification_permission_off")
                stopSelf()
                return START_NOT_STICKY
            }
            try {
                promoteToForeground(notification)
                foregroundPromoted = true
                KeepAliveServiceState.markStarted(applicationContext, reason)
                KeepAliveNotificationHelper.resetRecoveryAttempts(applicationContext)
                if (reason == KeepAliveNotificationHelper.FCM_HIGH_PRIORITY_REASON) {
                    Log.w(
                        KeepAliveNotificationHelper.FCM_FOREGROUND_SERVICE_LOG_TAG,
                        "FCM_FOREGROUND_SERVICE_STARTED startForeground completed successfully",
                    )
                }
            } catch (e: Exception) {
                handleStartFailure(reason, e, allowStartFailureRecovery)
                return START_NOT_STICKY
            }
            runServiceStep("start_in_process_timers") {
                InProcessTimerManager.start(applicationContext)
            }
            runServiceStep("restore_broadcast_receivers") {
                FlutterLocalNotificationPluginsPlugin.restoreBroadcastReceivers(applicationContext)
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
            runServiceStep("start_heartbeat") {
                heartbeatHandler.removeCallbacks(heartbeatRunnable)
                heartbeatHandler.post(heartbeatRunnable)
            }
            runServiceStep("start_gallery_observer") {
                GalleryImageObserverHelper.start(applicationContext)
            }
            return START_STICKY
        } catch (e: Exception) {
            foregroundPromoted = false
            KeepAliveServiceState.markIdle(applicationContext, "service_fatal:$reason")
            Log.e(
                TAG,
                "onStartCommand fatal reason=$reason error=${e.javaClass.simpleName}:${e.message}",
                e,
            )
            if (allowStartFailureRecovery) {
                scheduleRecovery("service_fatal:$reason")
            } else {
                KeepAliveServiceState.markStopping("service_fatal_no_recovery:$reason")
            }
            stopSelf()
            return START_NOT_STICKY
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            Log.d(TAG, "onTaskRemoved")
            TASK_REMOVED_CHECK_DELAYS.forEach { delayMillis ->
                heartbeatHandler.postDelayed(
                    {
                        try {
                            verifyAndRefreshForegroundNotification("task_removed_${delayMillis}ms")
                        } catch (e: Exception) {
                            Log.e(TAG, "taskRemoved refresh failed delay=$delayMillis", e)
                            runCatching { scheduleRecovery("task_removed_refresh_exception") }
                        }
                    },
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
            foregroundPromoted = false
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
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "onTimeout failed startId=$startId fgsType=$fgsType", e)
        } finally {
            runCatching { stopSelf(startId) }
        }
    }

    private fun scheduleRecovery(reason: String) {
        if (recoveryScheduled) return
        recoveryScheduled = true
        KeepAliveNotificationHelper.scheduleRecoveryRetry(applicationContext, reason)
    }

    private fun handleStartFailure(
        reason: String,
        error: Exception,
        allowRecovery: Boolean,
    ) {
        foregroundPromoted = false
        KeepAliveServiceState.markIdle(applicationContext, "start_foreground_failed:$reason")
        Log.e(
            TAG,
            "onStartCommand foreground promotion failed reason=$reason error=${error.javaClass.simpleName}:${error.message}",
            error,
        )
        if (reason == KeepAliveNotificationHelper.FCM_HIGH_PRIORITY_REASON) {
            Log.e(
                KeepAliveNotificationHelper.FCM_FOREGROUND_SERVICE_LOG_TAG,
                "FCM_FOREGROUND_SERVICE_NOT_STARTED startForeground failed: ${error.javaClass.simpleName}:${error.message}",
            )
        }
        if (allowRecovery) {
            scheduleRecovery("service_start_failed:$reason")
        } else {
            KeepAliveServiceState.markStopping("service_start_failed_no_recovery:$reason")
        }
        stopSelf()
    }

    private fun logFcmStartFailureIfNeeded(
        reason: String,
        detail: String,
    ) {
        if (reason != KeepAliveNotificationHelper.FCM_HIGH_PRIORITY_REASON) return
        Log.e(
            KeepAliveNotificationHelper.FCM_FOREGROUND_SERVICE_LOG_TAG,
            "FCM_FOREGROUND_SERVICE_NOT_STARTED $detail",
        )
    }

    private fun verifyAndRefreshForegroundNotification(reason: String) {
        try {
            if (KeepAliveServiceState.state != KeepAliveServiceState.State.STARTED) return
            val before = KeepAliveServiceState.describeForegroundNotification(applicationContext)
            val notification =
                KeepAliveNotificationHelper.buildPersistentShortcutNotification(applicationContext)
                    ?: return
            promoteToForeground(notification)
            KeepAliveServiceState.heartbeat(applicationContext)
            val after = KeepAliveServiceState.describeForegroundNotification(applicationContext)
            Log.d(TAG, "verifyAndRefresh reason=$reason before={$before} after={$after}")
        } catch (e: Exception) {
            Log.e(TAG, "verifyAndRefresh failed reason=$reason", e)
            runCatching { KeepAliveServiceState.markIdle(applicationContext, "refresh_failed:$reason") }
            runCatching { scheduleRecovery("refresh_failed:$reason") }
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
