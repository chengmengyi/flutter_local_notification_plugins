package com.local.notification.flutter_local_notification_plugins

import com.google.firebase.messaging.RemoteMessage

internal object FcmForegroundServicePolicy {
    fun shouldStart(deliveredPriority: Int): Boolean =
        deliveredPriority == RemoteMessage.PRIORITY_HIGH
}
