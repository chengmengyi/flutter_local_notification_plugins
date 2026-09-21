package com.local.notification.flutter_local_notification_plugins

import kotlin.random.Random

internal object CloseOverlayProbabilityDecider {
    data class Decision(
        val closeOverlayProbability: Int,
        val randomPercent: Int,
        val shouldReallyClose: Boolean,
    )

    fun roll(closeOverlayProbability: Int): Decision {
        val normalizedProbability = closeOverlayProbability.coerceIn(0, 100)
        val randomPercent = Random.nextInt(100)
        return Decision(
            closeOverlayProbability = normalizedProbability,
            randomPercent = randomPercent,
            shouldReallyClose =
                shouldReallyClose(
                    closeOverlayProbability = normalizedProbability,
                    randomPercent = randomPercent,
                ),
        )
    }

    fun shouldReallyClose(
        closeOverlayProbability: Int,
        randomPercent: Int,
    ): Boolean {
        require(randomPercent in 0..99) { "randomPercent must be from 0 to 99" }
        return randomPercent < closeOverlayProbability.coerceIn(0, 100)
    }
}
