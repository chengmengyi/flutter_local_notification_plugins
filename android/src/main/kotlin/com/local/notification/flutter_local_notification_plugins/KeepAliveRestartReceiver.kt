package com.local.notification.flutter_local_notification_plugins

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class KeepAliveRestartReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "KeepAliveRestartRcvr"
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        try {
            if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
                Log.d(TAG, "onReceive blocked")
                return
            }
            val reason = intent.getStringExtra("restart_reason")
            Log.d(TAG, "onReceive reason=$reason")
            KeepAliveNotificationHelper.handleRestartReceiver(context, reason)
        } catch (e: Exception) {
            Log.d(TAG, "onReceive failed error=${e.message}")
        }
    }
}
