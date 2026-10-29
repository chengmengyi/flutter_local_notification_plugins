package com.local.notification.flutter_local_notification_plugins

import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class NotificationConfigTest {
    private fun validValues(): Map<String, Any?> =
        mapOf(
            "allPushCdMilliseconds" to 60_000L,
            "localIntervalMilliseconds" to 3_600_000L,
            "actionIntervalMilliseconds" to 600_000L,
            "batteryChangedIntervalMilliseconds" to 1_200_000L,
            "pushMaxTime" to 30,
            "pushMaxBroadcast" to 20,
            "pushRefreshIntervalMilliseconds" to 2_000L,
            "pushNotifyTotalCount" to 30,
            "closeSystemDialogsDelayTimeMilliseconds" to 3_000L,
            "cleanPush" to true,
            "cleanPushTimeMilliseconds" to 1_800_000L,
        )

    @Test
    fun parsesAllRequiredValues() {
        val config = NotificationConfig.fromMap(validValues())

        assertEquals(60_000L, config.allPushCooldownMillis)
        assertEquals(30, config.pushNotifyTotalCount)
        assertEquals(3_000L, config.closeSystemDialogsDelayTimeMillis)
        assertEquals(true, config.cleanPush)
        assertEquals(1_800_000L, config.cleanPushTimeMillis)
    }

    @Test
    fun rejectsMissingRequiredCloseSystemDialogsDelay() {
        assertFailsWith<IllegalArgumentException> {
            NotificationConfig.fromMap(validValues() - "closeSystemDialogsDelayTimeMilliseconds")
        }
    }

    @Test
    fun rejectsRefreshesWithoutPositiveInterval() {
        assertFailsWith<IllegalArgumentException> {
            NotificationConfig.fromMap(
                validValues() + ("pushRefreshIntervalMilliseconds" to 0L),
            )
        }
    }

    @Test
    fun rejectsCleaningWithoutPositiveTimeout() {
        assertFailsWith<IllegalArgumentException> {
            NotificationConfig.fromMap(
                validValues() + ("cleanPushTimeMilliseconds" to 0L),
            )
        }
    }

    @Test
    fun allowsZeroTimeoutWhenCleaningIsDisabled() {
        val config =
            NotificationConfig.fromMap(
                validValues() +
                    mapOf(
                        "cleanPush" to false,
                        "cleanPushTimeMilliseconds" to 0L,
                    ),
            )

        assertEquals(false, config.cleanPush)
        assertEquals(0L, config.cleanPushTimeMillis)
    }

    @Test
    fun readsLegacyStoredConfigWithCleaningDisabled() {
        val legacy =
            JSONObject(NotificationConfig.DEFAULT.toJson()).apply {
                remove("cleanPush")
                remove("cleanPushTimeMillis")
            }

        val config = NotificationConfig.fromJson(legacy.toString())

        assertEquals(false, config.cleanPush)
        assertEquals(0L, config.cleanPushTimeMillis)
    }
}
