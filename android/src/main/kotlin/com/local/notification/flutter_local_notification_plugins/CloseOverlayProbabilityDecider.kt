package com.local.notification.flutter_local_notification_plugins

import kotlin.random.Random

internal object CloseOverlayProbabilityDecider {
    fun roll(closeOverlayProbability: Int): Boolean =
        shouldReallyClose(
            closeOverlayProbability = closeOverlayProbability,
            randomPercent = Random.nextInt(100),
        )

    fun shouldReallyClose(
        closeOverlayProbability: Int,
        randomPercent: Int,
    ): Boolean {
        require(randomPercent in 0..99) { "randomPercent must be from 0 to 99" }
        return randomPercent < closeOverlayProbability.coerceIn(0, 100)
    }
}
