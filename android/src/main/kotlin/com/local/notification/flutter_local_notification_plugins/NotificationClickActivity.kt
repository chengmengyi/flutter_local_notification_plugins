package com.local.notification.flutter_local_notification_plugins

import android.app.Activity
import android.os.Bundle

class NotificationClickActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleClick()
        finish()
    }

    private fun handleClick() {
        FlutterLocalNotificationPluginsPlugin.cancelClickedNotification(
            applicationContext,
            intent,
        )
        FlutterLocalNotificationPluginsPlugin.bringHostAppToForegroundOrStart(applicationContext)
    }
}
