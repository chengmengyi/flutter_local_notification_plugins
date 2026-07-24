package com.local.notification.flutter_local_notification_plugins

import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class NativePushReporterTest {
    @Test
    fun countKeyOccurrences_findsKeysAtAnyDepth() {
        val template = JSONObject(
            """{
                "top":"value",
                "nested":{"target":"first"},
                "items":[{"target":"second"}]
            }""",
        )

        assertEquals(1, NativePushReporter.countKeyOccurrences(template, "top"))
        assertEquals(2, NativePushReporter.countKeyOccurrences(template, "target"))
        assertEquals(0, NativePushReporter.countKeyOccurrences(template, "missing"))
    }

    @Test
    fun replaceKeyRecursively_replacesNestedObjectValueInPlace() {
        val template = JSONObject("""{"rsvp":{"conclave":"old"},"frozen":"push"}""")

        assertTrue(NativePushReporter.replaceKeyRecursively(template, "conclave", "new"))

        assertEquals("new", template.getJSONObject("rsvp").getString("conclave"))
        assertFalse(template.has("conclave"))
    }

    @Test
    fun replaceKeyRecursively_replacesValueInsideArray() {
        val template = JSONObject("""{"items":[{"clientTs":0}]}""")

        assertTrue(NativePushReporter.replaceKeyRecursively(template, "clientTs", 123L))

        assertEquals(123L, template.getJSONArray("items").getJSONObject(0).getLong("clientTs"))
    }

    @Test
    fun replaceKeyRecursively_returnsFalseWhenKeyIsMissing() {
        val template = JSONObject("""{"nested":{"other":"value"}}""")

        assertFalse(NativePushReporter.replaceKeyRecursively(template, "missing", "new"))
    }
}
