package com.local.notification.flutter_local_notification_plugins

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

internal object SessionBackgroundReporter {
    private const val TAG = "SessionBackground"
    private const val PREFS_NAME = "session_background_reporting"
    private const val KEY_CONFIG = "config"
    private const val KEY_BACKGROUND_ACTIVE = "background_active"
    private const val KEY_LAST_SUCCESS_AT = "last_success_at"
    private const val KEY_WORK_TOKEN = "work_token"
    private const val INPUT_WORK_TOKEN = "workToken"
    private const val INPUT_REASON = "reason"
    private const val WORK_TAG = "session_background_hourly_work"
    private const val MAX_ATTEMPTS = 3
    private const val HOUR_MILLIS = 60L * 60L * 1000L

    @Synchronized
    fun configure(
        context: Context,
        arguments: Map<*, *>,
    ) {
        val appContext = context.applicationContext
        val enabled = arguments["enabled"] as? Boolean ?: false
        if (!enabled) {
            disable(appContext)
            Log.i(TAG, "disabled")
            return
        }

        val url = arguments["url"]?.toString().orEmpty()
        require(URL(url).protocol.equals("https", ignoreCase = true)) {
            "Session background reporting URL must use HTTPS"
        }
        val headers = JSONObject(arguments["headers"] as? Map<*, *> ?: emptyMap<Any, Any>())
        val template =
            JSONObject(arguments["payloadTemplate"] as? Map<*, *> ?: emptyMap<Any, Any>())
        val dynamicKeys =
            listOf(
                "distinctIdKey",
                "logIdKey",
                "clientTsKey",
                "packageKey",
            ).associateWith { name ->
                arguments[name]?.toString()?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("$name must not be empty")
            }
        require(dynamicKeys.values.toSet().size == dynamicKeys.size) {
            "Session background reporting dynamic keys must be different"
        }
        dynamicKeys.values.forEach { key ->
            val occurrences = NativePushReporter.countKeyOccurrences(template, key)
            require(occurrences > 0) { "payloadTemplate does not contain key: $key" }
            require(occurrences == 1) {
                "payloadTemplate contains key more than once: $key"
            }
        }

        val config =
            JSONObject()
                .put("enabled", true)
                .put("url", url)
                .put("headers", headers)
                .put("payloadTemplate", template)
        dynamicKeys.forEach { (name, value) -> config.put(name, value) }
        prefs(appContext).edit().putString(KEY_CONFIG, config.toString()).commit()
        if (FlutterLocalNotificationPluginsPlugin.isHostActivityInForeground()) {
            onForeground(appContext)
        } else {
            onBackground(appContext)
        }
        Log.i(TAG, "configured url=$url")
    }

    /** Starts a new background session and immediately attempts its first report. */
    @Synchronized
    fun onBackground(context: Context) {
        val appContext = context.applicationContext
        if (readConfig(appContext) == null) return
        prefs(appContext)
            .edit()
            .putBoolean(KEY_BACKGROUND_ACTIVE, true)
            .remove(KEY_LAST_SUCCESS_AT)
            .commit()
        enqueueCheck(appContext, delayMillis = 0L, reason = "entered_background")
        Log.i(TAG, "background session started")
    }

    /** Stops the current session. A later background transition starts from zero again. */
    @Synchronized
    fun onForeground(context: Context) {
        val appContext = context.applicationContext
        if (readConfig(appContext) == null) return
        prefs(appContext)
            .edit()
            .putBoolean(KEY_BACKGROUND_ACTIVE, false)
            .remove(KEY_LAST_SUCCESS_AT)
            .putString(KEY_WORK_TOKEN, UUID.randomUUID().toString())
            .commit()
        WorkManager.getInstance(appContext).cancelAllWorkByTag(WORK_TAG)
        Log.i(TAG, "background session stopped by foreground")
    }

    /** A keep-alive service process revival is reported before any notification-related checks. */
    @Synchronized
    fun onKeepAliveStarted(context: Context) {
        val appContext = context.applicationContext
        if (!isBackgroundActive(appContext) || readConfig(appContext) == null) return
        prefs(appContext).edit().remove(KEY_LAST_SUCCESS_AT).commit()
        enqueueCheck(
            appContext,
            delayMillis = 0L,
            reason = "keep_alive_revived",
        )
        Log.i(TAG, "keep-alive revival queued immediate report")
    }

    internal fun isReportDue(
        lastSuccessAt: Long,
        now: Long,
    ): Boolean = lastSuccessAt <= 0L || now < lastSuccessAt || now - lastSuccessAt >= HOUR_MILLIS

    internal fun remainingDelayMillis(
        lastSuccessAt: Long,
        now: Long,
    ): Long {
        if (isReportDue(lastSuccessAt, now)) return 0L
        return (HOUR_MILLIS - (now - lastSuccessAt)).coerceAtLeast(0L)
    }

    internal fun runWorker(
        context: Context,
        token: String,
        reason: String,
        attempt: Int,
    ): WorkerResult {
        val appContext = context.applicationContext
        val config = readConfig(appContext) ?: return WorkerResult.SUCCESS
        if (!isCurrentActiveToken(appContext, token)) return WorkerResult.SUCCESS
        if (FlutterLocalNotificationPluginsPlugin.isHostActivityInForeground()) {
            onForeground(appContext)
            return WorkerResult.SUCCESS
        }

        val now = System.currentTimeMillis()
        val lastSuccessAt = prefs(appContext).getLong(KEY_LAST_SUCCESS_AT, 0L)
        if (!isReportDue(lastSuccessAt, now)) {
            enqueueCheck(
                appContext,
                delayMillis = remainingDelayMillis(lastSuccessAt, now),
                reason = "wait_until_hourly_due",
            )
            return WorkerResult.SUCCESS
        }

        return try {
            val body = buildBody(appContext, config, token, now)
            Log.i(TAG, "POST start reason=$reason attempt=$attempt/$MAX_ATTEMPTS body=$body")
            val statusCode = post(config, body)
            when {
                statusCode in 200..299 -> {
                    Log.i(TAG, "POST success reason=$reason status=$statusCode")
                    recordSuccessAndScheduleNext(appContext, token, System.currentTimeMillis())
                    WorkerResult.SUCCESS
                }
                shouldRetry(statusCode) && attempt < MAX_ATTEMPTS -> {
                    Log.w(TAG, "POST retry reason=$reason status=$statusCode")
                    WorkerResult.RETRY
                }
                else -> {
                    Log.e(TAG, "POST failed reason=$reason status=$statusCode")
                    scheduleAfterFailure(appContext, token)
                    WorkerResult.SUCCESS
                }
            }
        } catch (error: Exception) {
            if (attempt < MAX_ATTEMPTS) {
                Log.w(TAG, "POST exception retry reason=$reason error=${error.message}", error)
                WorkerResult.RETRY
            } else {
                Log.e(TAG, "POST exception exhausted reason=$reason error=${error.message}", error)
                scheduleAfterFailure(appContext, token)
                WorkerResult.SUCCESS
            }
        }
    }

    @Synchronized
    private fun recordSuccessAndScheduleNext(
        context: Context,
        token: String,
        successAt: Long,
    ) {
        if (!isCurrentActiveToken(context, token)) return
        prefs(context).edit().putLong(KEY_LAST_SUCCESS_AT, successAt).commit()
        enqueueCheck(context, delayMillis = HOUR_MILLIS, reason = "hourly")
    }

    @Synchronized
    private fun scheduleAfterFailure(
        context: Context,
        token: String,
    ) {
        if (!isCurrentActiveToken(context, token)) return
        enqueueCheck(context, delayMillis = HOUR_MILLIS, reason = "retry_after_failed_cycle")
    }

    private fun buildBody(
        context: Context,
        config: JSONObject,
        logId: String,
        clientTs: Long,
    ): JSONObject {
        val body = JSONObject(config.getJSONObject("payloadTemplate").toString())
        val replacements =
            mapOf(
                config.getString("distinctIdKey") to NativePushReporter.getDistinctId(context),
                config.getString("logIdKey") to logId,
                config.getString("clientTsKey") to clientTs,
                config.getString("packageKey") to context.packageName,
            )
        replacements.forEach { (key, value) ->
            check(NativePushReporter.replaceKeyRecursively(body, key, value)) {
                "payloadTemplate does not contain key: $key"
            }
        }
        return body
    }

    private fun post(
        config: JSONObject,
        body: JSONObject,
    ): Int {
        val connection = URL(config.getString("url")).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            val headers = config.getJSONObject("headers")
            var hasContentType = false
            headers.keys().forEach { key ->
                if (key.equals("content-type", ignoreCase = true)) hasContentType = true
                connection.setRequestProperty(key, headers.getString(key))
            }
            if (!hasContentType) {
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val status = connection.responseCode
            runCatching {
                (if (status >= 400) connection.errorStream else connection.inputStream)
                    ?.close()
            }
            status
        } finally {
            connection.disconnect()
        }
    }

    @Synchronized
    private fun enqueueCheck(
        context: Context,
        delayMillis: Long,
        reason: String,
    ) {
        if (!isBackgroundActive(context) || readConfig(context) == null) return
        val token = UUID.randomUUID().toString()
        prefs(context).edit().putString(KEY_WORK_TOKEN, token).commit()
        val request =
            OneTimeWorkRequestBuilder<SessionBackgroundWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(INPUT_WORK_TOKEN, token)
                        .putString(INPUT_REASON, reason)
                        .build(),
                )
                .setInitialDelay(delayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10L, TimeUnit.SECONDS)
                .addTag(WORK_TAG)
                .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    @Synchronized
    private fun disable(context: Context) {
        prefs(context).edit().clear().commit()
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
    }

    private fun isCurrentActiveToken(
        context: Context,
        token: String,
    ): Boolean =
        isBackgroundActive(context) &&
            token.isNotBlank() &&
            prefs(context).getString(KEY_WORK_TOKEN, null) == token

    private fun isBackgroundActive(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BACKGROUND_ACTIVE, false)

    private fun readConfig(context: Context): JSONObject? =
        prefs(context).getString(KEY_CONFIG, null)?.let {
            runCatching { JSONObject(it) }.getOrNull()
        }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun shouldRetry(statusCode: Int): Boolean =
        statusCode == 408 || statusCode == 429 || statusCode >= 500

    internal enum class WorkerResult {
        SUCCESS,
        RETRY,
    }
}

internal class SessionBackgroundWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val token = inputData.getString("workToken") ?: return Result.success()
        val reason = inputData.getString("reason") ?: "unknown"
        return when (
            SessionBackgroundReporter.runWorker(
                context = applicationContext,
                token = token,
                reason = reason,
                attempt = runAttemptCount + 1,
            )
        ) {
            SessionBackgroundReporter.WorkerResult.SUCCESS -> Result.success()
            SessionBackgroundReporter.WorkerResult.RETRY -> Result.retry()
        }
    }
}
