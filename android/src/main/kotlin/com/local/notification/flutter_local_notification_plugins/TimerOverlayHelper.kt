package com.local.notification.flutter_local_notification_plugins

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

object TimerOverlayHelper {
    private const val TAG = "TimerOverlayHelper"
    private const val PREFS_NAME = "flutter_local_notification_plugins"
    private const val KEY_TIMER_OVERLAY_LAYOUT = "timer_overlay_layout"
    private const val KEY_TIMER_OVERLAY_CONTENT_LIST = "timer_overlay_content_list"
    private const val KEY_TIMER_OVERLAY_INTERVAL_MILLIS = "timer_overlay_interval_millis"
    private const val KEY_TIMER_OVERLAY_LAST_PDF_TITLE = "timer_overlay_last_pdf_title"
    private const val KEY_TIMER_OVERLAY_LAST_PDF_PAGE = "timer_overlay_last_pdf_page"
    private const val KEY_TIMER_OVERLAY_LAST_PDF_SUBTITLE_TEMPLATE =
        "timer_overlay_last_pdf_subtitle_template"
    private const val KEY_TIMER_OVERLAY_LAST_PDF_BUTTON_TEXT =
        "timer_overlay_last_pdf_button_text"
    private const val KEY_TIMER_OVERLAY_CLICK_EVENT = "timer_overlay_click_event"
    private const val TIMER_OVERLAY_ACTION =
        "com.local.notification.flutter_local_notification_plugins.TIMER_OVERLAY"
    private const val TIMER_OVERLAY_REQUEST_CODE = 12006
    private const val DEFAULT_TIMER_OVERLAY_INTERVAL_MILLIS = 20L * 60L * 1000L
    private const val PART_SEPARATOR = "\u0001"

    fun saveConfig(
        context: Context,
        layoutName: String,
        contentList: List<Map<String, Any?>>,
        requestedIntervalMillis: Long?,
        lastPdfSubtitleTemplate: String?,
        lastPdfButtonText: String?,
    ) {
        val rows =
            contentList.mapNotNull { item ->
                val title = item["title"]?.toString()?.trim().orEmpty()
                val desc =
                    (
                        item["subtitle"]
                            ?: item["desc"]
                    )?.toString()?.trim().orEmpty()
                val button = item["button"]?.toString()?.trim().orEmpty()
                if (title.isBlank() && desc.isBlank() && button.isBlank()) {
                    null
                } else {
                    listOf(title, desc, button).joinToString(PART_SEPARATOR)
                }
            }
        if (layoutName.isBlank() || rows.isEmpty()) {
            Log.d(TAG, "saveConfig skipped layoutName=$layoutName count=${rows.size}")
            cancel(context)
            return
        }
        prefs(context)
            .edit()
            .putString(KEY_TIMER_OVERLAY_LAYOUT, layoutName)
            .putString(KEY_TIMER_OVERLAY_CONTENT_LIST, JSONArray(rows).toString())
            .putString(
                KEY_TIMER_OVERLAY_LAST_PDF_SUBTITLE_TEMPLATE,
                lastPdfSubtitleTemplate?.trim().orEmpty(),
            )
            .putString(
                KEY_TIMER_OVERLAY_LAST_PDF_BUTTON_TEXT,
                lastPdfButtonText?.trim().orEmpty(),
            )
            .putLong(
                KEY_TIMER_OVERLAY_INTERVAL_MILLIS,
                resolveIntervalMillis(context, requestedIntervalMillis),
            )
            .apply()
        scheduleNext(context)
        Log.d(TAG, "saveConfig success layoutName=$layoutName count=${rows.size}")
    }

    fun saveLastPdfInfo(
        context: Context,
        title: String,
        pageNumber: Int,
    ) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank() || pageNumber <= 0) {
            clearLastPdfInfo(context)
            return
        }
        prefs(context)
            .edit()
            .putString(KEY_TIMER_OVERLAY_LAST_PDF_TITLE, normalizedTitle)
            .putInt(KEY_TIMER_OVERLAY_LAST_PDF_PAGE, pageNumber)
            .apply()
    }

    fun resolveDisplayContent(
        context: Context,
        title: String,
        desc: String,
        button: String,
    ): TimerOverlayDisplayContent {
        val lastPdfInfo = maybeConsumeLastPdfInfo(context)
        if (lastPdfInfo != null) {
            return TimerOverlayDisplayContent(
                title = lastPdfInfo.title,
                desc = lastPdfInfo.subtitle,
                button = lastPdfInfo.button.takeIf { it.isNotBlank() } ?: button,
                shouldClearLastPdfInfoAfterDisplay = true,
            )
        }
        return TimerOverlayDisplayContent(title = title, desc = desc, button = button)
    }

    fun clearLastPdfInfoAfterDisplay(context: Context) {
        clearLastPdfInfo(context)
    }

    fun cacheAndDispatchClickEvent(
        context: Context,
        layoutName: String,
        content: TimerOverlayDisplayContent?,
    ) {
        val event =
            mapOf(
                "timestamp" to System.currentTimeMillis(),
                "layoutName" to layoutName,
                "title" to content?.title,
                "subtitle" to content?.desc,
                "button" to content?.button,
                "appState" to (
                    if (FlutterLocalNotificationPluginsPlugin.isHostActivityInForeground()) {
                        "foreground"
                    } else {
                        "background_or_cold_start"
                    }
                ),
            )
        prefs(context)
            .edit()
            .putString(KEY_TIMER_OVERLAY_CLICK_EVENT, JSONObject(event).toString())
            .apply()
        FlutterLocalNotificationPluginsPlugin.dispatchTimerOverlayClicked(context, event)
    }

    fun consumeClickEvent(context: Context): Map<String, Any?>? {
        val raw = prefs(context).getString(KEY_TIMER_OVERLAY_CLICK_EVENT, null) ?: return null
        prefs(context).edit().remove(KEY_TIMER_OVERLAY_CLICK_EVENT).apply()
        return try {
            val json = JSONObject(raw)
            mapOf(
                "timestamp" to json.optLong("timestamp"),
                "layoutName" to json.optString("layoutName").takeIf { it.isNotBlank() },
                "title" to json.optString("title").takeIf { it.isNotBlank() },
                "subtitle" to json.optString("subtitle").takeIf { it.isNotBlank() },
                "button" to json.optString("button").takeIf { it.isNotBlank() },
                "appState" to json.optString("appState").takeIf { it.isNotBlank() },
            )
        } catch (e: Exception) {
            Log.d(TAG, "consumeClickEvent failed error=${e.message}")
            null
        }
    }

    fun handleAlarm(context: Context) {
        val config = readConfig(context)
        if (config == null) {
            cancel(context)
            return
        }
        scheduleNext(context)
        if (FlutterLocalNotificationPluginsPlugin.isHostActivityInForeground()) {
            Log.d(TAG, "handleAlarm skipped, app foreground")
            return
        }
        TimerOverlayService.show(
            context = context,
            layoutName = config.layoutName,
            title = config.content.title,
            desc = config.content.desc,
            button = config.content.button,
        )
    }

    fun scheduleNext(context: Context) {
        if (readConfig(context) == null) {
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = createPendingIntent(context)
        val intervalMillis = readIntervalMillis(context)
        val triggerAt = System.currentTimeMillis() + intervalMillis
        alarmManager.cancel(pendingIntent)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent,
            )
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
        Log.d(TAG, "scheduleNext intervalMillis=$intervalMillis triggerAt=$triggerAt")
    }

    fun pause(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.cancel(createPendingIntent(context))
        TimerOverlayService.close(context)
        Log.d(TAG, "pause")
    }

    fun resume(context: Context) {
        if (readConfig(context) == null) {
            Log.d(TAG, "resume skipped, no config")
            return
        }
        scheduleNext(context)
        Log.d(TAG, "resume")
    }

    fun cancel(context: Context) {
        prefs(context)
            .edit()
            .remove(KEY_TIMER_OVERLAY_LAYOUT)
            .remove(KEY_TIMER_OVERLAY_CONTENT_LIST)
            .remove(KEY_TIMER_OVERLAY_INTERVAL_MILLIS)
            .remove(KEY_TIMER_OVERLAY_LAST_PDF_TITLE)
            .remove(KEY_TIMER_OVERLAY_LAST_PDF_PAGE)
            .remove(KEY_TIMER_OVERLAY_LAST_PDF_SUBTITLE_TEMPLATE)
            .remove(KEY_TIMER_OVERLAY_LAST_PDF_BUTTON_TEXT)
            .remove(KEY_TIMER_OVERLAY_CLICK_EVENT)
            .apply()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.cancel(createPendingIntent(context))
        TimerOverlayService.close(context)
    }

    private fun readConfig(context: Context): TimerOverlayConfig? {
        val sharedPrefs = prefs(context)
        val layoutName = sharedPrefs.getString(KEY_TIMER_OVERLAY_LAYOUT, null)
            ?.takeUnless { it.isBlank() }
            ?: return null
        val rows = readContentRows(sharedPrefs.getString(KEY_TIMER_OVERLAY_CONTENT_LIST, null))
        if (rows.isEmpty()) {
            return null
        }
        val raw = rows[Random.nextInt(rows.size)]
        val parts = raw.split(PART_SEPARATOR)
        return TimerOverlayConfig(
            layoutName = layoutName,
            content =
                TimerOverlayContent(
                    title = parts.getOrNull(0).orEmpty(),
                    desc = parts.getOrNull(1).orEmpty(),
                    button = parts.getOrNull(2).orEmpty(),
                ),
        )
    }

    private fun createPendingIntent(context: Context): PendingIntent {
        val intent =
            Intent(context, TimerOverlayReceiver::class.java).apply {
                action = TIMER_OVERLAY_ACTION
            }
        return PendingIntent.getBroadcast(
            context,
            TIMER_OVERLAY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun readContentRows(raw: String?): List<String> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            val array = JSONArray(raw)
            List(array.length()) { index -> array.optString(index) }
                .filter { it.isNotBlank() }
        } catch (_: Exception) {
            raw.split("\n").filter { it.isNotBlank() }
        }
    }

    private fun maybeConsumeLastPdfInfo(context: Context): LastPdfInfo? {
        if (!Random.nextBoolean()) {
            return null
        }
        val sharedPrefs = prefs(context)
        val title = sharedPrefs.getString(KEY_TIMER_OVERLAY_LAST_PDF_TITLE, null)
            ?.trim()
            .orEmpty()
        val pageNumber = sharedPrefs.getInt(KEY_TIMER_OVERLAY_LAST_PDF_PAGE, 0)
        if (title.isBlank() || pageNumber <= 0) {
            return null
        }
        val subtitleTemplate =
            sharedPrefs.getString(KEY_TIMER_OVERLAY_LAST_PDF_SUBTITLE_TEMPLATE, null)
                ?.takeUnless { it.isBlank() }
                ?: "You were on page {n}. Let's finish it!"
        val buttonText = sharedPrefs.getString(KEY_TIMER_OVERLAY_LAST_PDF_BUTTON_TEXT, null)
            ?.trim()
            .orEmpty()
        return LastPdfInfo(
            title = title,
            subtitle = subtitleTemplate.replace("{n}", pageNumber.toString()),
            button = buttonText,
        )
    }

    private fun clearLastPdfInfo(context: Context) {
        prefs(context)
            .edit()
            .remove(KEY_TIMER_OVERLAY_LAST_PDF_TITLE)
            .remove(KEY_TIMER_OVERLAY_LAST_PDF_PAGE)
            .apply()
    }

    private fun resolveIntervalMillis(
        context: Context,
        requestedIntervalMillis: Long?,
    ): Long {
        val isDebuggable =
            context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (!isDebuggable) {
            return DEFAULT_TIMER_OVERLAY_INTERVAL_MILLIS
        }
        return requestedIntervalMillis
            ?.takeIf { it > 0L }
            ?: DEFAULT_TIMER_OVERLAY_INTERVAL_MILLIS
    }

    private fun readIntervalMillis(context: Context): Long {
        return prefs(context).getLong(
            KEY_TIMER_OVERLAY_INTERVAL_MILLIS,
            DEFAULT_TIMER_OVERLAY_INTERVAL_MILLIS,
        ).takeIf { it > 0L } ?: DEFAULT_TIMER_OVERLAY_INTERVAL_MILLIS
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private data class TimerOverlayConfig(
        val layoutName: String,
        val content: TimerOverlayContent,
    )

    data class TimerOverlayDisplayContent(
        val title: String,
        val desc: String,
        val button: String,
        val shouldClearLastPdfInfoAfterDisplay: Boolean = false,
    )

    private data class TimerOverlayContent(
        val title: String,
        val desc: String,
        val button: String,
    )

    private data class LastPdfInfo(
        val title: String,
        val subtitle: String,
        val button: String,
    )
}
