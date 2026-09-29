package com.local.notification.flutter_local_notification_plugins

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class LocalFirebaseMessageService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val context = applicationContext
        val deliveredPriority = remoteMessage.priority
        val originalPriority = remoteMessage.originalPriority
        val isDeliveredHighPriority =
            FcmForegroundServicePolicy.shouldStart(deliveredPriority)

        if (isDeliveredHighPriority) {
            Log.w(
                KeepAliveNotificationHelper.FCM_FOREGROUND_SERVICE_LOG_TAG,
                "FCM_HIGH_PRIORITY_RECEIVED originalPriority=$originalPriority deliveredPriority=$deliveredPriority",
            )
            val startRequested =
                try {
                    KeepAliveNotificationHelper.startOrUpdateForegroundService(
                        context = context,
                        reason = KeepAliveNotificationHelper.FCM_HIGH_PRIORITY_REASON,
                        allowStartFailureRecovery = false,
                    )
                } catch (error: Exception) {
                    Log.e(
                        KeepAliveNotificationHelper.FCM_FOREGROUND_SERVICE_LOG_TAG,
                        "FCM_FOREGROUND_SERVICE_NOT_STARTED unexpected error=${error.javaClass.simpleName}:${error.message}",
                        error,
                    )
                    false
                }
            Log.d(
                "LocalNotificationPlugin",
                "FCM foreground service startRequested=$startRequested originalPriority=$originalPriority deliveredPriority=$deliveredPriority",
            )
            if (!startRequested) {
                Log.e(
                    KeepAliveNotificationHelper.FCM_FOREGROUND_SERVICE_LOG_TAG,
                    "FCM_FOREGROUND_SERVICE_NOT_STARTED start request was rejected or skipped",
                )
            }
        } else {
            Log.d(
                "LocalNotificationPlugin",
                "FCM foreground service skipped originalPriority=$originalPriority deliveredPriority=$deliveredPriority",
            )
        }

        try {
            val data = remoteMessage.data
            val title =
                data["title"]
                    ?: remoteMessage.notification?.title
                    ?: ""
            val body =
                data["body"]
                    ?: remoteMessage.notification?.body
                    ?: ""
            val messageId =
                data["id"]?.toIntOrNull()
                    ?: ((System.currentTimeMillis() % Int.MAX_VALUE).toInt())
            FlutterLocalNotificationPluginsPlugin.showFcmNotification(
                context = context,
                title = title,
                body = body,
                messageId = messageId,
                allowForegroundServiceStart = isDeliveredHighPriority,
            )
            Log.d(
                "LocalNotificationPlugin",
                "LocalFirebaseMessageService.onMessageReceived messageId=$messageId title=$title",
            )
        } catch (e: Exception) {
            Log.d(
                "LocalNotificationPlugin",
                "LocalFirebaseMessageService.onMessageReceived failed error=${e.message}",
            )
        }
    }
}
