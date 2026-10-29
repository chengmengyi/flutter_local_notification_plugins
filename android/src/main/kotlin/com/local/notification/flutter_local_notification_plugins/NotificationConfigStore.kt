package com.local.notification.flutter_local_notification_plugins

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import org.json.JSONObject

internal data class NotificationConfig(
    val allPushCooldownMillis: Long,
    val localIntervalMillis: Long,
    val actionIntervalMillis: Long,
    val batteryChangedIntervalMillis: Long,
    val pushMaxTime: Int,
    val pushMaxBroadcast: Int,
    val pushRefreshIntervalMillis: Long,
    val pushNotifyTotalCount: Int,
    val closeSystemDialogsDelayTimeMillis: Long,
    val cleanPush: Boolean,
    val cleanPushTimeMillis: Long,
) {
    companion object {
        val DEFAULT =
            NotificationConfig(
                allPushCooldownMillis = 30_000L,
                localIntervalMillis = 30L * 60L * 1000L,
                actionIntervalMillis = 30L * 60L * 1000L,
                batteryChangedIntervalMillis = 30L * 60L * 1000L,
                pushMaxTime = 0,
                pushMaxBroadcast = 0,
                pushRefreshIntervalMillis = 2_500L,
                pushNotifyTotalCount = 5,
                closeSystemDialogsDelayTimeMillis = 0L,
                cleanPush = false,
                cleanPushTimeMillis = 0L,
            )

        fun fromMap(values: Map<String, Any?>): NotificationConfig {
            fun requiredLong(key: String): Long =
                (values[key] as? Number)?.toLong()
                    ?: throw IllegalArgumentException("$key is required")

            fun requiredBoolean(key: String): Boolean =
                values[key] as? Boolean
                    ?: throw IllegalArgumentException("$key is required")

            val config =
                NotificationConfig(
                    allPushCooldownMillis = requiredLong("allPushCdMilliseconds"),
                    localIntervalMillis = requiredLong("localIntervalMilliseconds"),
                    actionIntervalMillis = requiredLong("actionIntervalMilliseconds"),
                    batteryChangedIntervalMillis = requiredLong("batteryChangedIntervalMilliseconds"),
                    pushMaxTime = requiredLong("pushMaxTime").toValidatedInt("pushMaxTime"),
                    pushMaxBroadcast = requiredLong("pushMaxBroadcast").toValidatedInt("pushMaxBroadcast"),
                    pushRefreshIntervalMillis = requiredLong("pushRefreshIntervalMilliseconds"),
                    pushNotifyTotalCount =
                        requiredLong("pushNotifyTotalCount").toValidatedInt("pushNotifyTotalCount"),
                    closeSystemDialogsDelayTimeMillis =
                        requiredLong("closeSystemDialogsDelayTimeMilliseconds"),
                    cleanPush = requiredBoolean("cleanPush"),
                    cleanPushTimeMillis = requiredLong("cleanPushTimeMilliseconds"),
                )
            config.validate()
            return config
        }

        fun fromJson(raw: String): NotificationConfig {
            val json = JSONObject(raw)
            return NotificationConfig(
                allPushCooldownMillis = json.getLong("allPushCooldownMillis"),
                localIntervalMillis = json.getLong("localIntervalMillis"),
                actionIntervalMillis = json.getLong("actionIntervalMillis"),
                batteryChangedIntervalMillis = json.getLong("batteryChangedIntervalMillis"),
                pushMaxTime = json.getInt("pushMaxTime"),
                pushMaxBroadcast = json.getInt("pushMaxBroadcast"),
                pushRefreshIntervalMillis = json.getLong("pushRefreshIntervalMillis"),
                pushNotifyTotalCount = json.getInt("pushNotifyTotalCount"),
                closeSystemDialogsDelayTimeMillis =
                    if (json.has("closeSystemDialogsDelayTimeMillis")) {
                        json.getLong("closeSystemDialogsDelayTimeMillis")
                    } else {
                        json.getLong("closeSystemDialogsDelayTimeSeconds") * 1000L
                    },
                cleanPush = json.optBoolean("cleanPush", false),
                cleanPushTimeMillis = json.optLong("cleanPushTimeMillis", 0L),
            ).also { it.validate() }
        }

        private fun Long.toValidatedInt(key: String): Int {
            if (this !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                throw IllegalArgumentException("$key is out of range")
            }
            return toInt()
        }
    }

    fun toJson(): String =
        JSONObject()
            .put("allPushCooldownMillis", allPushCooldownMillis)
            .put("localIntervalMillis", localIntervalMillis)
            .put("actionIntervalMillis", actionIntervalMillis)
            .put("batteryChangedIntervalMillis", batteryChangedIntervalMillis)
            .put("pushMaxTime", pushMaxTime)
            .put("pushMaxBroadcast", pushMaxBroadcast)
            .put("pushRefreshIntervalMillis", pushRefreshIntervalMillis)
            .put("pushNotifyTotalCount", pushNotifyTotalCount)
            .put("closeSystemDialogsDelayTimeMillis", closeSystemDialogsDelayTimeMillis)
            .put("cleanPush", cleanPush)
            .put("cleanPushTimeMillis", cleanPushTimeMillis)
            .toString()

    private fun validate() {
        require(allPushCooldownMillis >= 0L) { "allPushCdMilliseconds must be >= 0" }
        require(localIntervalMillis > 0L) { "localIntervalMilliseconds must be > 0" }
        require(actionIntervalMillis >= 0L) { "actionIntervalMilliseconds must be >= 0" }
        require(batteryChangedIntervalMillis >= 0L) {
            "batteryChangedIntervalMilliseconds must be >= 0"
        }
        require(pushMaxTime >= 0) { "pushMaxTime must be >= 0" }
        require(pushMaxBroadcast >= 0) { "pushMaxBroadcast must be >= 0" }
        require(pushRefreshIntervalMillis >= 0L) {
            "pushRefreshIntervalMilliseconds must be >= 0"
        }
        require(pushNotifyTotalCount > 0) { "pushNotifyTotalCount must be > 0" }
        require(pushNotifyTotalCount == 1 || pushRefreshIntervalMillis > 0L) {
            "pushRefreshIntervalMilliseconds must be > 0 when pushNotifyTotalCount > 1"
        }
        require(closeSystemDialogsDelayTimeMillis >= 0L) {
            "closeSystemDialogsDelayTimeMilliseconds must be >= 0"
        }
        require(cleanPushTimeMillis >= 0L) { "cleanPushTimeMilliseconds must be >= 0" }
        require(!cleanPush || cleanPushTimeMillis > 0L) {
            "cleanPushTimeMilliseconds must be > 0 when cleanPush is true"
        }
    }
}

internal object NotificationConfigStore {
    private const val TAG = "NotificationConfig"
    private const val PREFS_NAME = "flutter_local_notification_plugins"
    private const val KEY_CONFIG = "notification_config_v1"

    fun save(
        context: Context,
        values: Map<String, Any?>,
    ): NotificationConfig {
        val config = NotificationConfig.fromMap(values)
        val configJson = config.toJson()
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CONFIG, configJson)
            .apply()
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Log.d(TAG, "saved config=$configJson")
        }
        return config
    }

    fun read(context: Context): NotificationConfig {
        val raw =
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CONFIG, null)
                ?: return NotificationConfig.DEFAULT
        return runCatching { NotificationConfig.fromJson(raw) }.getOrDefault(NotificationConfig.DEFAULT)
    }
}
