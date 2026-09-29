package com.local.notification.flutter_local_notification_plugins

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class SessionBackgroundReporterTest {
    private val hourMillis = 60L * 60L * 1000L

    @Test
    fun noSuccessfulReport_isDueImmediately() {
        assertTrue(SessionBackgroundReporter.isReportDue(0L, 1_000L))
    }

    @Test
    fun reportBecomesDueAfterOneHour() {
        val lastSuccessAt = 10_000L
        assertFalse(
            SessionBackgroundReporter.isReportDue(
                lastSuccessAt,
                lastSuccessAt + hourMillis - 1L,
            ),
        )
        assertTrue(
            SessionBackgroundReporter.isReportDue(
                lastSuccessAt,
                lastSuccessAt + hourMillis,
            ),
        )
    }

    @Test
    fun remainingDelay_countsDownFromLastSuccess() {
        val lastSuccessAt = 10_000L
        assertEquals(
            15L * 60L * 1000L,
            SessionBackgroundReporter.remainingDelayMillis(
                lastSuccessAt,
                lastSuccessAt + 45L * 60L * 1000L,
            ),
        )
    }

    @Test
    fun clockRollback_isDueInsteadOfBlockingForever() {
        assertTrue(SessionBackgroundReporter.isReportDue(20_000L, 10_000L))
    }
}
