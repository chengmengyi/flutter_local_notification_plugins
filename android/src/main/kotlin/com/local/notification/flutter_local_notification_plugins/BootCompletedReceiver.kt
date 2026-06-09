package com.local.notification.flutter_local_notification_plugins

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action ?: "unknown"
        try {
            Log.d("LocalNotificationPlugin", "BootCompletedReceiver.onReceive action=$action")
            FlutterLocalNotificationPluginsPlugin.restoreAfterBoot(
                context = context,
                reason = action,
            )
        } catch (e: Exception) {
            Log.d("LocalNotificationPlugin", "BootCompletedReceiver failed action=$action error=${e.message}")
        }
    }
}
