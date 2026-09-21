package com.local.notification.flutter_local_notification_plugins

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class CloseOverlayProbabilityDeciderTest {
    @Test
    fun zeroProbability_neverReallyCloses() {
        assertFalse(CloseOverlayProbabilityDecider.shouldReallyClose(0, 0))
        assertFalse(CloseOverlayProbabilityDecider.shouldReallyClose(0, 99))
    }

    @Test
    fun fullProbability_alwaysReallyCloses() {
        assertTrue(CloseOverlayProbabilityDecider.shouldReallyClose(100, 0))
        assertTrue(CloseOverlayProbabilityDecider.shouldReallyClose(100, 99))
    }

    @Test
    fun twentyPercent_closesForSamplesZeroThroughNineteen() {
        assertTrue(CloseOverlayProbabilityDecider.shouldReallyClose(20, 0))
        assertTrue(CloseOverlayProbabilityDecider.shouldReallyClose(20, 19))
        assertFalse(CloseOverlayProbabilityDecider.shouldReallyClose(20, 20))
        assertFalse(CloseOverlayProbabilityDecider.shouldReallyClose(20, 99))
    }
}
