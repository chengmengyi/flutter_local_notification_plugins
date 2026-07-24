package com.local.notification.flutter_local_notification_plugins

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

private object NativePushReportingLog {
    private const val TAG = "NativePushReporter"

    fun info(message: String) {
        Log.i(TAG, message)
        System.out.println("[$TAG] $message")
    }

    fun warn(message: String, error: Throwable? = null) {
        Log.w(TAG, message, error)
        System.out.println("[$TAG] WARN $message")
        error?.printStackTrace(System.out)
    }

    fun error(message: String, error: Throwable? = null) {
        Log.e(TAG, message, error)
        System.err.println("[$TAG] ERROR $message")
        error?.printStackTrace(System.err)
    }
}

internal object NativePushReporter {
    private const val PREFS_NAME = "native_push_reporting"
    private const val KEY_CONFIG = "config"
    private const val EVENT_PREFIX = "event_"

    @Synchronized
    fun configure(context: Context, arguments: Map<*, *>) {
        val enabled = arguments["enabled"] as? Boolean ?: false
        if (!enabled) {
            prefs(context).edit().remove(KEY_CONFIG).apply()
            NativePushReportingLog.info("reporting disabled")
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
            "logIdKey",
            "clientTsKey",
            "notificationSourceKey",
            "packageKey",
        ).associateWith { name ->
            arguments[name]?.toString()?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("$name must not be empty")
        }
        require(dynamicKeys.values.toSet().size == dynamicKeys.size) {
            "Native push reporting dynamic keys must be different"
        }
        dynamicKeys.values.forEach { key ->
            val occurrences = countKeyOccurrences(template, key)
            require(occurrences > 0) { "payloadTemplate does not contain key: $key" }
            require(occurrences == 1) {
                "payloadTemplate contains key more than once: $key"
            }
        }
        val config = JSONObject()
            .put("enabled", true)
            .put("url", url)
            .put("headers", headers)
            .put("payloadTemplate", template)
        dynamicKeys.forEach { (name, value) -> config.put(name, value) }
        prefs(context).edit().putString(KEY_CONFIG, config.toString()).apply()
        NativePushReportingLog.info("reporting configured url=$url")
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
            val replacements = mapOf(
                config.getString("distinctIdKey") to getDistinctId(appContext),
                config.getString("logIdKey") to UUID.randomUUID().toString(),
                config.getString("clientTsKey") to System.currentTimeMillis(),
                config.getString("notificationSourceKey") to source,
                config.getString("packageKey") to appContext.packageName,
            )
            replacements.forEach { (key, value) ->
                check(replaceKeyRecursively(body, key, value)) {
                    "payloadTemplate does not contain key: $key"
                }
            }
            eventId = UUID.randomUUID().toString()
            val event = JSONObject()
                .put("createdAt", System.currentTimeMillis())
                .put("notificationSource", source)
                .put("url", config.getString("url"))
                .put("headers", config.getJSONObject("headers"))
                .put("body", body)
                .put("callbackArguments", JSONObject(arguments))
            saveEvent(appContext, eventId, event)
            enqueue(appContext, eventId)
            NativePushReportingLog.info("report queued source=$source eventId=$eventId")
        } catch (error: Exception) {
            eventId?.let { removeEvent(appContext, it) }
            NativePushReportingLog.warn("Unable to enqueue native push report", error)
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

    internal fun countKeyOccurrences(value: Any?, targetKey: String): Int =
        when (value) {
            is JSONObject -> {
                value.keys().asSequence().sumOf { key ->
                    (if (key == targetKey) 1 else 0) +
                        countKeyOccurrences(value.opt(key), targetKey)
                }
            }
            is JSONArray -> {
                (0 until value.length()).sumOf { index ->
                    countKeyOccurrences(value.opt(index), targetKey)
                }
            }
            else -> 0
        }

    internal fun replaceKeyRecursively(
        value: Any?,
        targetKey: String,
        replacement: Any?,
    ): Boolean =
        when (value) {
            is JSONObject -> {
                var replaced = false
                value.keys().asSequence().toList().forEach { key ->
                    if (key == targetKey) {
                        value.put(key, replacement)
                        replaced = true
                    } else if (replaceKeyRecursively(value.opt(key), targetKey, replacement)) {
                        replaced = true
                    }
                }
                replaced
            }
            is JSONArray -> {
                var replaced = false
                for (index in 0 until value.length()) {
                    if (replaceKeyRecursively(value.opt(index), targetKey, replacement)) {
                        replaced = true
                    }
                }
                replaced
            }
            else -> false
        }

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
        val source = event.optString("notificationSource")
        val attempt = runAttemptCount + 1
        val url = event.optString("url")
        val requestBody = event.optJSONObject("body")?.toString().orEmpty()
        NativePushReportingLog.info(
            "POST start source=$source attempt=$attempt/$MAX_ATTEMPTS url=$url body=$requestBody",
        )
        return try {
            val statusCode = post(event)
            when {
                statusCode in 200..299 -> {
                    NativePushReportingLog.info(
                        "POST success source=$source attempt=$attempt/$MAX_ATTEMPTS status=$statusCode",
                    )
                    NativePushReporter.removeEvent(applicationContext, eventId)
                    Result.success()
                }
                shouldRetry(statusCode) && attempt < MAX_ATTEMPTS -> {
                    NativePushReportingLog.warn(
                        "POST retry source=$source attempt=$attempt/$MAX_ATTEMPTS status=$statusCode",
                    )
                    Result.retry()
                }
                else -> {
                    NativePushReportingLog.error(
                        "POST failed source=$source attempt=$attempt/$MAX_ATTEMPTS status=$statusCode fallback=true",
                    )
                    NativePushReporter.finalFallback(applicationContext, eventId, event)
                    Result.success()
                }
            }
        } catch (error: Exception) {
            if (attempt < MAX_ATTEMPTS) {
                NativePushReportingLog.warn(
                    "POST exception source=$source attempt=$attempt/$MAX_ATTEMPTS retry=true error=${error.message}",
                    error,
                )
                Result.retry()
            } else {
                NativePushReportingLog.error(
                    "POST exception source=$source attempt=$attempt/$MAX_ATTEMPTS fallback=true error=${error.message}",
                    error,
                )
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
            val responseBody = runCatching {
                (if (status >= 400) connection.errorStream else connection.inputStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
            }.getOrDefault("")
            NativePushReportingLog.info(
                "POST response source=${event.optString("notificationSource")} status=$status body=${responseBody.take(4000)}",
            )
            status
        } finally {
            connection.disconnect()
        }
    }

    private fun shouldRetry(statusCode: Int): Boolean =
        statusCode == 408 || statusCode == 429 || statusCode >= 500
}
