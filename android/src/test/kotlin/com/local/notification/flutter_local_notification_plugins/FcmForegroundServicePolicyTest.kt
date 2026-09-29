package com.local.notification.flutter_local_notification_plugins

import com.google.firebase.messaging.RemoteMessage
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class FcmForegroundServicePolicyTest {
    @Test
    fun highDeliveredPriority_allowsForegroundServiceStart() {
        assertTrue(
            FcmForegroundServicePolicy.shouldStart(RemoteMessage.PRIORITY_HIGH),
        )
    }

    @Test
    fun normalDeliveredPriority_blocksForegroundServiceStart() {
        assertFalse(
            FcmForegroundServicePolicy.shouldStart(RemoteMessage.PRIORITY_NORMAL),
        )
    }

    @Test
    fun unknownDeliveredPriority_blocksForegroundServiceStart() {
        assertFalse(
            FcmForegroundServicePolicy.shouldStart(RemoteMessage.PRIORITY_UNKNOWN),
        )
    }
}
