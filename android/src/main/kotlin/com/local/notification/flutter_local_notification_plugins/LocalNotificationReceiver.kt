package com.local.notification.flutter_local_notification_plugins

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class LocalNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
            Log.d("LocalNotificationPlugin", "LocalNotificationReceiver blocked")
            return
        }
        Log.d("LocalNotificationPlugin", "LocalNotificationReceiver.onReceive")
        FlutterLocalNotificationPluginsPlugin.showNotificationFromIntent(context, intent)
    }
}
