package com.local.notification.flutter_local_notification_plugins

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class NotificationRefreshSessionGateTest {
    @Test
    fun claim_acceptsOnlyTheCurrentTokenInOrder() {
        val gate = NotificationRefreshSessionGate()
        gate.start("current", 4)

        assertFalse(gate.claim("other", 1))
        assertFalse(gate.claim("current", 2))
        assertTrue(gate.claim("current", 1))
        assertTrue(gate.claim("current", 2))
        assertTrue(gate.claim("current", 3))
        assertTrue(gate.claim("current", 4))
        assertFalse(gate.claim("current", 5))
    }

    @Test
    fun start_invalidatesThePreviousSession() {
        val gate = NotificationRefreshSessionGate()
        gate.start("first", 4)
        gate.start("second", 2)

        assertFalse(gate.isCurrent("first"))
        assertFalse(gate.claim("first", 1))
        assertTrue(gate.isCurrent("second"))
        assertTrue(gate.claim("second", 1))
    }

    @Test
    fun finishAndCancel_clearTheCurrentSession() {
        val gate = NotificationRefreshSessionGate()
        gate.start("first", 4)

        assertFalse(gate.finish("other"))
        assertTrue(gate.finish("first"))
        assertFalse(gate.isCurrent("first"))

        gate.start("second", 4)
        assertTrue(gate.cancel())
        assertFalse(gate.cancel())
        assertFalse(gate.isCurrent("second"))
    }
}
