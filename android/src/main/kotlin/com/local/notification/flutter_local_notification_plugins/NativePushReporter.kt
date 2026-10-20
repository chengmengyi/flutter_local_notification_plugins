package com.local.notification.flutter_local_notification_plugins

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID

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
    private const val CONNECT_TIMEOUT_MILLIS = 60_000
    private const val READ_TIMEOUT_MILLIS = 60_000

    private val reportingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            val request = JSONObject()
                .put("notificationSource", source)
                .put("url", config.getString("url"))
                .put("headers", config.getJSONObject("headers"))
                .put("body", body)
            reportingScope.launch {
                postOnce(request)
            }
            NativePushReportingLog.info("report scheduled source=$source")
        } catch (error: Exception) {
            NativePushReportingLog.warn("Unable to schedule native push report", error)
        }
    }

    private fun readConfig(context: Context): JSONObject? =
        prefs(context).getString(KEY_CONFIG, null)?.let {
            runCatching { JSONObject(it) }.getOrNull()
        }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun postOnce(request: JSONObject) {
        val source = request.optString("notificationSource")
        val url = request.optString("url")
        val requestBody = request.optJSONObject("body")?.toString().orEmpty()
        NativePushReportingLog.info(
            "POST start source=$source url=$url body=$requestBody",
        )
        try {
            val statusCode = post(request)
            if (statusCode in 200..299) {
                NativePushReportingLog.info(
                    "POST success source=$source status=$statusCode",
                )
            } else {
                NativePushReportingLog.error(
                    "POST failed source=$source status=$statusCode retry=false",
                )
            }
        } catch (error: Exception) {
            NativePushReportingLog.error(
                "POST exception source=$source retry=false error=${error.message}",
                error,
            )
        }
    }

    private fun post(request: JSONObject): Int {
        val connection = URL(request.getString("url")).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.doOutput = true
            val headers = request.getJSONObject("headers")
            var hasContentType = false
            headers.keys().forEach { key ->
                if (key.equals("content-type", ignoreCase = true)) hasContentType = true
                connection.setRequestProperty(key, headers.getString(key))
            }
            if (!hasContentType) {
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            connection.outputStream.use {
                it.write(request.getJSONObject("body").toString().toByteArray())
            }
            val status = connection.responseCode
            val responseBody = runCatching {
                (if (status >= 400) connection.errorStream else connection.inputStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
            }.getOrDefault("")
            NativePushReportingLog.info(
                "POST response source=${request.optString("notificationSource")} " +
                    "status=$status body=${responseBody.take(4000)}",
            )
            status
        } finally {
            connection.disconnect()
        }
    }

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
    internal fun getDistinctId(context: Context): String {
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
