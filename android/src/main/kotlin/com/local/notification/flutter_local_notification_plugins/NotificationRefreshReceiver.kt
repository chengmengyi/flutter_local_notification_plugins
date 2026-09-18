package com.local.notification.flutter_local_notification_plugins

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        runCatching {
            NotificationRefreshCoordinator.handleRefreshIntent(context, intent)
        }.onFailure { error ->
            Log.e("NotificationRefresh", "receiver failed", error)
            NotificationRefreshCoordinator.cancel("receiver_failed")
        }
    }
}
