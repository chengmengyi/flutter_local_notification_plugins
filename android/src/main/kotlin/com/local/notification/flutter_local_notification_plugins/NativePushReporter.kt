package com.local.notification.flutter_local_notification_plugins

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

internal object NativePushReporter {
    private const val TAG = "NativePushReporter"
    private const val PREFS_NAME = "native_push_reporting"
    private const val KEY_CONFIG = "config"
    private const val EVENT_PREFIX = "event_"

    @Synchronized
    fun configure(context: Context, arguments: Map<*, *>) {
        val enabled = arguments["enabled"] as? Boolean ?: false
        if (!enabled) {
            prefs(context).edit().remove(KEY_CONFIG).apply()
            return
        }
        val url = arguments["url"]?.toString().orEmpty()
        require(URL(url).protocol.equals("https", ignoreCase = true)) {
            "Native push reporting URL must use HTTPS"
        }
        val headers = JSONObject(arguments["headers"] as? Map<*, *> ?: emptyMap<Any, Any>())
        val template = JSONObject(arguments["payloadTemplate"] as? Map<*, *> ?: emptyMap<Any, Any>())
        val dynamicKeys = listOf(
            "distinctIdKey",
            "eventIdKey",
            "timestampKey",
            "notificationSourceKey",
        ).associateWith { name ->
            arguments[name]?.toString()?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("$name must not be empty")
        }
        require(dynamicKeys.values.toSet().size == dynamicKeys.size) {
            "Native push reporting dynamic keys must be different"
        }
        dynamicKeys.values.forEach { key ->
            require(template.has(key)) { "payloadTemplate does not contain key: $key" }
        }
        val config = JSONObject()
            .put("enabled", true)
            .put("url", url)
            .put("headers", headers)
            .put("payloadTemplate", template)
        dynamicKeys.forEach { (name, value) -> config.put(name, value) }
        prefs(context).edit().putString(KEY_CONFIG, config.toString()).apply()
    }

    fun reportDisplayed(context: Context, arguments: Map<String, Any?>) {
        val appContext = context.applicationContext
        val config = readConfig(appContext)
        if (config == null) {
            FlutterLocalNotificationPluginsPlugin.dispatchNotificationDisplayed(appContext, arguments)
            return
        }
        var eventId: String? = null
        try {
            val source = arguments["payload"]?.toString().orEmpty()
            val body = JSONObject(config.getJSONObject("payloadTemplate").toString())
                .put(config.getString("distinctIdKey"), getDistinctId(appContext))
                .put(config.getString("eventIdKey"), UUID.randomUUID().toString())
                .put(config.getString("timestampKey"), System.currentTimeMillis())
                .put(config.getString("notificationSourceKey"), source)
            eventId = UUID.randomUUID().toString()
            val event = JSONObject()
                .put("createdAt", System.currentTimeMillis())
                .put("url", config.getString("url"))
                .put("headers", config.getJSONObject("headers"))
                .put("body", body)
                .put("callbackArguments", JSONObject(arguments))
            saveEvent(appContext, eventId, event)
            enqueue(appContext, eventId)
        } catch (error: Exception) {
            eventId?.let { removeEvent(appContext, it) }
            Log.w(TAG, "Unable to enqueue native push report", error)
            FlutterLocalNotificationPluginsPlugin.dispatchNotificationDisplayed(appContext, arguments)
        }
    }

    fun readEvent(context: Context, eventId: String): JSONObject? =
        prefs(context).getString(EVENT_PREFIX + eventId, null)?.let {
            runCatching { JSONObject(it) }.getOrNull()
        }

    @Synchronized
    fun removeEvent(context: Context, eventId: String) {
        prefs(context).edit().remove(EVENT_PREFIX + eventId).apply()
    }

    fun finalFallback(context: Context, eventId: String, event: JSONObject) {
        val rawArguments = event.optJSONObject("callbackArguments") ?: JSONObject()
        val arguments = mutableMapOf<String, Any?>()
        rawArguments.keys().forEach { key ->
            arguments[key] = rawArguments.opt(key).takeUnless { it == JSONObject.NULL }
        }
        removeEvent(context, eventId)
        FlutterLocalNotificationPluginsPlugin.dispatchNotificationDisplayed(context, arguments)
    }

    private fun readConfig(context: Context): JSONObject? =
        prefs(context).getString(KEY_CONFIG, null)?.let {
            runCatching { JSONObject(it) }.getOrNull()
        }

    @Synchronized
    private fun saveEvent(context: Context, eventId: String, event: JSONObject) {
        prefs(context).edit().putString(EVENT_PREFIX + eventId, event.toString()).apply()
    }

    private fun enqueue(context: Context, eventId: String) {
        val request = OneTimeWorkRequestBuilder<NativePushReportWorker>()
            .setInputData(Data.Builder().putString("eventId", eventId).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "native_push_report_$eventId",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Matches flutter_tba_info 0.0.9: MD5 of ANDROID_ID, excluding the known broken ID.
    private fun getDistinctId(context: Context): String {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty().takeUnless { it == "9774d56d682e549c" }.orEmpty()
        return runCatching {
            MessageDigest.getInstance("MD5").digest(androidId.toByteArray()).joinToString("") {
                "%02x".format(it.toInt() and 0xff)
            }
        }.getOrDefault(androidId)
    }
}

internal class NativePushReportWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {
    companion object {
        private const val MAX_ATTEMPTS = 3
    }

    override fun doWork(): Result {
        val eventId = inputData.getString("eventId") ?: return Result.success()
        val event = NativePushReporter.readEvent(applicationContext, eventId) ?: return Result.success()
        return try {
            val statusCode = post(event)
            when {
                statusCode in 200..299 -> {
                    NativePushReporter.removeEvent(applicationContext, eventId)
                    Result.success()
                }
                shouldRetry(statusCode) && runAttemptCount + 1 < MAX_ATTEMPTS -> Result.retry()
                else -> {
                    NativePushReporter.finalFallback(applicationContext, eventId, event)
                    Result.success()
                }
            }
        } catch (error: Exception) {
            if (runAttemptCount + 1 < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                NativePushReporter.finalFallback(applicationContext, eventId, event)
                Result.success()
            }
        }
    }

    private fun post(event: JSONObject): Int {
        val connection = URL(event.getString("url")).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            val headers = event.getJSONObject("headers")
            var hasContentType = false
            headers.keys().forEach { key ->
                if (key.equals("content-type", ignoreCase = true)) hasContentType = true
                connection.setRequestProperty(key, headers.getString(key))
            }
            if (!hasContentType) connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(event.getJSONObject("body").toString().toByteArray()) }
            val status = connection.responseCode
            runCatching {
                (if (status >= 400) connection.errorStream else connection.inputStream)?.use { it.readBytes() }
            }
            status
        } finally {
            connection.disconnect()
        }
    }

    private fun shouldRetry(statusCode: Int): Boolean =
        statusCode == 408 || statusCode == 429 || statusCode >= 500
}
