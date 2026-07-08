package com.local.notification.flutter_local_notification_plugins

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object TimerOverlayHelper {
    private const val TAG = "TimerOverlayHelper"
    private const val PREFS_NAME = "flutter_local_notification_plugins"
    private const val KEY_TIMER_OVERLAY_LAYOUT = "timer_overlay_layout"
    private const val KEY_TIMER_OVERLAY_CONTENT_LIST = "timer_overlay_content_list"
    private const val KEY_TIMER_OVERLAY_LAYOUT_2 = "timer_overlay_layout_2"
    private const val KEY_TIMER_OVERLAY_CONTENT_LIST_2 = "timer_overlay_content_list_2"
    private const val KEY_TIMER_OVERLAY_INTERVAL_MILLIS = "timer_overlay_interval_millis"
    private const val KEY_TIMER_OVERLAY_INTERVAL_FROM_UPDATE = "timer_overlay_interval_from_update"
    private const val KEY_TIMER_OVERLAY_LAST_PDF_TITLE = "timer_overlay_last_pdf_title"
    private const val KEY_TIMER_OVERLAY_LAST_PDF_PAGE = "timer_overlay_last_pdf_page"
    private const val KEY_TIMER_OVERLAY_CONTINUE_READING_STR =
        "timer_overlay_continue_reading_str"
    private const val KEY_TIMER_OVERLAY_LAST_PDF_SUBTITLE_TEMPLATE =
        "timer_overlay_last_pdf_subtitle_template"
    private const val KEY_TIMER_OVERLAY_LAST_PDF_BUTTON_TEXT =
        "timer_overlay_last_pdf_button_text"
    private const val KEY_TIMER_OVERLAY_CLICK_EVENT = "timer_overlay_click_event"
    private const val KEY_TIMER_OVERLAY_ONE_DAY_MAX_COUNT = "timer_overlay_one_day_max_count"
    private const val KEY_TIMER_OVERLAY_DISPLAY_COUNT_DATE = "timer_overlay_display_count_date"
    private const val KEY_TIMER_OVERLAY_DISPLAY_COUNT = "timer_overlay_display_count"
    private const val KEY_TIMER_OVERLAY_CD_TIME_MINUTES = "timer_overlay_cd_time_minutes"
    private const val KEY_TIMER_OVERLAY_LAST_DISPLAY_AT = "timer_overlay_last_display_at"
    private const val KEY_TIMER_OVERLAY_REFLECTION_SECRET = "timer_overlay_reflection_secret"
    private const val KEY_TIMER_OVERLAY_REFLECTION_SETTINGS_CLASS =
        "timer_overlay_reflection_settings_class"
    private const val KEY_TIMER_OVERLAY_REFLECTION_CAN_DRAW_OVERLAYS_METHOD =
        "timer_overlay_reflection_can_draw_overlays_method"
    private const val KEY_TIMER_OVERLAY_REFLECTION_CONTEXT_GET_SYSTEM_SERVICE_METHOD =
        "timer_overlay_reflection_context_get_system_service_method"
    private const val KEY_TIMER_OVERLAY_REFLECTION_WINDOW_SERVICE_NAME =
        "timer_overlay_reflection_window_service_name"
    private const val KEY_TIMER_OVERLAY_REFLECTION_WINDOW_MANAGER_LAYOUT_PARAMS_CLASS =
        "timer_overlay_reflection_window_manager_layout_params_class"
    private const val KEY_TIMER_OVERLAY_REFLECTION_VIEW_GROUP_LAYOUT_PARAMS_CLASS =
        "timer_overlay_reflection_view_group_layout_params_class"
    private const val KEY_TIMER_OVERLAY_REFLECTION_WINDOW_MANAGER_CLASS =
        "timer_overlay_reflection_window_manager_class"
    private const val KEY_TIMER_OVERLAY_REFLECTION_ADD_VIEW_METHOD =
        "timer_overlay_reflection_add_view_method"
    private const val KEY_TIMER_OVERLAY_REFLECTION_REMOVE_VIEW_METHOD =
        "timer_overlay_reflection_remove_view_method"
    private const val KEY_TIMER_OVERLAY_REFLECTION_GRAVITY_FIELD =
        "timer_overlay_reflection_gravity_field"
    private const val KEY_TIMER_OVERLAY_REFLECTION_X_FIELD =
        "timer_overlay_reflection_x_field"
    private const val KEY_TIMER_OVERLAY_REFLECTION_Y_FIELD =
        "timer_overlay_reflection_y_field"
    private const val TIMER_OVERLAY_ACTION =
        "com.local.notification.flutter_local_notification_plugins.TIMER_OVERLAY"
    private const val TIMER_OVERLAY_REQUEST_CODE = 12006
    private const val DEFAULT_TIMER_OVERLAY_INTERVAL_MILLIS = 20L * 60L * 1000L
    private const val DEFAULT_TIMER_OVERLAY_CD_TIME_MINUTES = 1
    private const val MINUTE_MILLIS = 60L * 1000L
    private const val PART_SEPARATOR = "\u0001"

    data class TimerOverlayReflectionConfig(
        val secret: String,
        val settingsClass: String,
        val canDrawOverlaysMethod: String,
        val contextGetSystemServiceMethod: String,
        val windowServiceName: String,
        val windowManagerLayoutParamsClass: String,
        val viewGroupLayoutParamsClass: String,
        val windowManagerClass: String,
        val addViewMethod: String,
        val removeViewMethod: String,
        val gravityField: String,
        val xField: String,
        val yField: String,
    ) {
        fun isValid(): Boolean {
            return secret.isNotBlank() &&
                settingsClass.isNotBlank() &&
                canDrawOverlaysMethod.isNotBlank() &&
                contextGetSystemServiceMethod.isNotBlank() &&
                windowServiceName.isNotBlank() &&
                windowManagerLayoutParamsClass.isNotBlank() &&
                viewGroupLayoutParamsClass.isNotBlank() &&
                windowManagerClass.isNotBlank() &&
                addViewMethod.isNotBlank() &&
                removeViewMethod.isNotBlank() &&
                gravityField.isNotBlank() &&
                xField.isNotBlank() &&
                yField.isNotBlank()
        }
    }

    fun saveConfig(
        context: Context,
        layoutName: String,
        contentList: List<Map<String, Any?>>,
        layoutName2: String?,
        contentList2: List<Map<String, Any?>>,
        requestedIntervalMillis: Long?,
        continueReadingStr: String?,
        lastPdfSubtitleTemplate: String?,
        lastPdfButtonText: String?,
        reflectionConfig: TimerOverlayReflectionConfig,
    ) {
        val rows = encodeContentRows(contentList)
        val rows2 = encodeContentRows(contentList2)
        if (layoutName.isBlank() || rows.isEmpty() || !reflectionConfig.isValid()) {
            Log.d(
                TAG,
                "saveConfig skipped layoutName=$layoutName count=${rows.size} reflectionValid=${reflectionConfig.isValid()}",
            )
            cancel(context)
            return
        }
        val editor = prefs(context)
            .edit()
            .putString(KEY_TIMER_OVERLAY_LAYOUT, layoutName)
            .putString(KEY_TIMER_OVERLAY_CONTENT_LIST, JSONArray(rows).toString())
            .putString(
                KEY_TIMER_OVERLAY_CONTINUE_READING_STR,
                continueReadingStr?.trim().orEmpty(),
            )
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
                resolveSaveConfigIntervalMillis(context, requestedIntervalMillis),
            )
            .putString(KEY_TIMER_OVERLAY_REFLECTION_SECRET, reflectionConfig.secret)
            .putString(KEY_TIMER_OVERLAY_REFLECTION_SETTINGS_CLASS, reflectionConfig.settingsClass)
            .putString(
                KEY_TIMER_OVERLAY_REFLECTION_CAN_DRAW_OVERLAYS_METHOD,
                reflectionConfig.canDrawOverlaysMethod,
            )
            .putString(
                KEY_TIMER_OVERLAY_REFLECTION_CONTEXT_GET_SYSTEM_SERVICE_METHOD,
                reflectionConfig.contextGetSystemServiceMethod,
            )
            .putString(
                KEY_TIMER_OVERLAY_REFLECTION_WINDOW_SERVICE_NAME,
                reflectionConfig.windowServiceName,
            )
            .putString(
                KEY_TIMER_OVERLAY_REFLECTION_WINDOW_MANAGER_LAYOUT_PARAMS_CLASS,
                reflectionConfig.windowManagerLayoutParamsClass,
            )
            .putString(
                KEY_TIMER_OVERLAY_REFLECTION_VIEW_GROUP_LAYOUT_PARAMS_CLASS,
                reflectionConfig.viewGroupLayoutParamsClass,
            )
            .putString(
                KEY_TIMER_OVERLAY_REFLECTION_WINDOW_MANAGER_CLASS,
                reflectionConfig.windowManagerClass,
            )
            .putString(KEY_TIMER_OVERLAY_REFLECTION_ADD_VIEW_METHOD, reflectionConfig.addViewMethod)
            .putString(
                KEY_TIMER_OVERLAY_REFLECTION_REMOVE_VIEW_METHOD,
                reflectionConfig.removeViewMethod,
            )
            .putString(KEY_TIMER_OVERLAY_REFLECTION_GRAVITY_FIELD, reflectionConfig.gravityField)
            .putString(KEY_TIMER_OVERLAY_REFLECTION_X_FIELD, reflectionConfig.xField)
            .putString(KEY_TIMER_OVERLAY_REFLECTION_Y_FIELD, reflectionConfig.yField)
        if (!layoutName2.isNullOrBlank() && rows2.isNotEmpty()) {
            editor
                .putString(KEY_TIMER_OVERLAY_LAYOUT_2, layoutName2.trim())
                .putString(KEY_TIMER_OVERLAY_CONTENT_LIST_2, JSONArray(rows2).toString())
        } else {
            editor
                .remove(KEY_TIMER_OVERLAY_LAYOUT_2)
                .remove(KEY_TIMER_OVERLAY_CONTENT_LIST_2)
        }
        editor.apply()
        scheduleNext(context)
        Log.d(TAG, "saveConfig success layoutName=$layoutName count=${rows.size} layoutName2=$layoutName2 count2=${rows2.size}")
    }

    fun updateConfig(
        context: Context,
        requestedIntervalMillis: Long?,
        oneDayMaxCount: Int?,
        cdTime: Int?,
    ) {
        val editor =
            prefs(context)
                .edit()
                .putLong(
                    KEY_TIMER_OVERLAY_INTERVAL_MILLIS,
                    resolveIntervalMillis(context, requestedIntervalMillis),
                )
                .putBoolean(KEY_TIMER_OVERLAY_INTERVAL_FROM_UPDATE, true)
        if (oneDayMaxCount == null) {
            editor.remove(KEY_TIMER_OVERLAY_ONE_DAY_MAX_COUNT)
        } else {
            editor.putInt(KEY_TIMER_OVERLAY_ONE_DAY_MAX_COUNT, oneDayMaxCount.coerceAtLeast(0))
        }
        editor.putInt(
            KEY_TIMER_OVERLAY_CD_TIME_MINUTES,
            (cdTime ?: DEFAULT_TIMER_OVERLAY_CD_TIME_MINUTES).coerceAtLeast(0),
        )
        editor.apply()
        scheduleNext(context)
        Log.d(
            TAG,
            "updateConfig intervalMillis=${readIntervalMillis(context)} oneDayMaxCount=$oneDayMaxCount cdTime=${readCdTimeMinutes(context)}",
        )
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
        button2: String?,
        useLastPdfInfo: Boolean,
    ): TimerOverlayDisplayContent {
        val lastPdfInfo = if (useLastPdfInfo) maybeConsumeLastPdfInfo(context) else null
        if (lastPdfInfo != null) {
            return TimerOverlayDisplayContent(
                title = lastPdfInfo.title,
                desc = lastPdfInfo.subtitle,
                button = lastPdfInfo.button.takeIf { it.isNotBlank() } ?: button,
                button2 = button2,
                shouldClearLastPdfInfoAfterDisplay = true,
            )
        }
        return TimerOverlayDisplayContent(title = title, desc = desc, button = button, button2 = button2)
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
                "button2" to content?.button2,
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
                "button2" to json.optString("button2").takeIf { it.isNotBlank() },
                "appState" to json.optString("appState").takeIf { it.isNotBlank() },
            )
        } catch (e: Exception) {
            Log.d(TAG, "consumeClickEvent failed error=${e.message}")
            null
        }
    }

    fun handleAlarm(context: Context) {
        FlutterLocalNotificationPluginsPlugin.showLocalTriggeredMediaNotification(
            context = context,
            reason = "before_timer_overlay_alarm",
            recordDisplayedBeforePermission = true,
        )
        val config = readConfig(context)
        if (config == null) {
            cancel(context)
            return
        }
        scheduleNext(context)
        tryShowOverlay(context, config, "alarm")
    }

    fun tryShowForMediaTrigger(
        context: Context,
        reason: String,
    ) {
        val config = readConfig(context)
        if (config == null) {
            Log.d(TAG, "tryShowForMediaTrigger skipped, no config reason=$reason")
            return
        }
        tryShowOverlay(context, config, "media_trigger:$reason")
    }

    private fun tryShowOverlay(
        context: Context,
        config: TimerOverlayConfig,
        source: String,
    ): Boolean {
        if (FlutterLocalNotificationPluginsPlugin.isHostActivityInForeground()) {
            Log.d(TAG, "tryShowOverlay skipped, app foreground source=$source")
            return false
        }
        if (!canDisplayToday(context)) {
            Log.d(TAG, "tryShowOverlay skipped, one day max count reached source=$source")
            return false
        }
        if (!canDisplayByCooldown(context)) {
            Log.d(TAG, "tryShowOverlay skipped, cooldown source=$source")
            return false
        }
        if (!canDrawOverlaysByReflection(context)) {
            Log.d(TAG, "tryShowOverlay skipped, overlay permission missing source=$source")
            return false
        }
        increaseTodayDisplayCount(context)
        saveLastDisplayAt(context)
        TimerOverlayService.show(
            context = context,
            layoutName = config.layoutName,
            title = config.content.title,
            desc = config.content.desc,
            button = config.content.button,
            button2 = config.content.button2,
            useLastPdfInfo = config.useLastPdfInfo,
            continueReadingStr = config.continueReadingStr,
        )
        Log.d(TAG, "tryShowOverlay success source=$source")
        return true
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
            .remove(KEY_TIMER_OVERLAY_LAYOUT_2)
            .remove(KEY_TIMER_OVERLAY_CONTENT_LIST_2)
            .remove(KEY_TIMER_OVERLAY_INTERVAL_MILLIS)
            .remove(KEY_TIMER_OVERLAY_INTERVAL_FROM_UPDATE)
            .remove(KEY_TIMER_OVERLAY_LAST_PDF_TITLE)
            .remove(KEY_TIMER_OVERLAY_LAST_PDF_PAGE)
            .remove(KEY_TIMER_OVERLAY_CONTINUE_READING_STR)
            .remove(KEY_TIMER_OVERLAY_LAST_PDF_SUBTITLE_TEMPLATE)
            .remove(KEY_TIMER_OVERLAY_LAST_PDF_BUTTON_TEXT)
            .remove(KEY_TIMER_OVERLAY_CLICK_EVENT)
            .remove(KEY_TIMER_OVERLAY_ONE_DAY_MAX_COUNT)
            .remove(KEY_TIMER_OVERLAY_DISPLAY_COUNT_DATE)
            .remove(KEY_TIMER_OVERLAY_DISPLAY_COUNT)
            .remove(KEY_TIMER_OVERLAY_CD_TIME_MINUTES)
            .remove(KEY_TIMER_OVERLAY_LAST_DISPLAY_AT)
            .remove(KEY_TIMER_OVERLAY_REFLECTION_SECRET)
            .remove(KEY_TIMER_OVERLAY_REFLECTION_SETTINGS_CLASS)
            .remove(KEY_TIMER_OVERLAY_REFLECTION_CAN_DRAW_OVERLAYS_METHOD)
            .remove(KEY_TIMER_OVERLAY_REFLECTION_CONTEXT_GET_SYSTEM_SERVICE_METHOD)
            .remove(KEY_TIMER_OVERLAY_REFLECTION_WINDOW_SERVICE_NAME)
            .remove(KEY_TIMER_OVERLAY_REFLECTION_WINDOW_MANAGER_LAYOUT_PARAMS_CLASS)
            .remove(KEY_TIMER_OVERLAY_REFLECTION_VIEW_GROUP_LAYOUT_PARAMS_CLASS)
            .remove(KEY_TIMER_OVERLAY_REFLECTION_WINDOW_MANAGER_CLASS)
            .remove(KEY_TIMER_OVERLAY_REFLECTION_ADD_VIEW_METHOD)
            .remove(KEY_TIMER_OVERLAY_REFLECTION_REMOVE_VIEW_METHOD)
            .remove(KEY_TIMER_OVERLAY_REFLECTION_GRAVITY_FIELD)
            .remove(KEY_TIMER_OVERLAY_REFLECTION_X_FIELD)
            .remove(KEY_TIMER_OVERLAY_REFLECTION_Y_FIELD)
            .apply()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.cancel(createPendingIntent(context))
        TimerOverlayService.close(context)
    }

    fun canDrawOverlaysByReflection(context: Context): Boolean {
        return runCatching {
            val config = readReflectionConfig(context) ?: return false
            val settingsClass = Class.forName(config.decode(config.settingsClass))
            val method =
                settingsClass.getMethod(
                    config.decode(config.canDrawOverlaysMethod),
                    Context::class.java,
                )
            method.invoke(null, context) as Boolean
        }.onFailure {
            Log.d(TAG, "canDrawOverlaysByReflection failed error=${it.message}")
        }.getOrDefault(false)
    }

    fun addViewByReflection(
        context: Context,
        view: View?,
        width: Int,
        height: Int,
        type: Int,
        flags: Int,
        format: Int,
        gravity: Int,
        x: Int,
        y: Int,
    ): Boolean {
        return runCatching {
            val config = readReflectionConfig(context) ?: return false
            val getSystemServiceMethod =
                Context::class.java.getMethod(
                    config.decode(config.contextGetSystemServiceMethod),
                    String::class.java,
                )
            val windowManager =
                getSystemServiceMethod.invoke(context, config.decode(config.windowServiceName))
            val layoutParamsClass =
                Class.forName(config.decode(config.windowManagerLayoutParamsClass))
            val constructor =
                layoutParamsClass.getConstructor(
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                )
            val layoutParams = constructor.newInstance(width, height, type, flags, format)
            layoutParamsClass.getField(config.decode(config.gravityField)).setInt(layoutParams, gravity)
            layoutParamsClass.getField(config.decode(config.xField)).setInt(layoutParams, x)
            layoutParamsClass.getField(config.decode(config.yField)).setInt(layoutParams, y)
            val windowManagerClass = Class.forName(config.decode(config.windowManagerClass))
            val addViewMethod =
                windowManagerClass.getMethod(
                    config.decode(config.addViewMethod),
                    View::class.java,
                    Class.forName(config.decode(config.viewGroupLayoutParamsClass)),
                )
            addViewMethod.invoke(windowManager, view, layoutParams)
            true
        }.onFailure {
            Log.d(TAG, "addViewByReflection failed error=${it.message}")
        }.getOrDefault(false)
    }

    fun removeViewByReflection(
        context: Context,
        view: View?,
    ): Boolean {
        if (view == null) {
            return false
        }
        return runCatching {
            val config = readReflectionConfig(context) ?: return false
            val getSystemServiceMethod =
                Context::class.java.getMethod(
                    config.decode(config.contextGetSystemServiceMethod),
                    String::class.java,
                )
            val windowManager =
                getSystemServiceMethod.invoke(context, config.decode(config.windowServiceName))
            val windowManagerClass = Class.forName(config.decode(config.windowManagerClass))
            val removeViewMethod =
                windowManagerClass.getMethod(
                    config.decode(config.removeViewMethod),
                    View::class.java,
                )
            removeViewMethod.invoke(windowManager, view)
            true
        }.onFailure {
            Log.d(TAG, "removeViewByReflection failed error=${it.message}")
        }.getOrDefault(false)
    }

    private fun readReflectionConfig(context: Context): TimerOverlayReflectionConfig? {
        val sharedPrefs = prefs(context)
        val config =
            TimerOverlayReflectionConfig(
                secret = sharedPrefs.getString(KEY_TIMER_OVERLAY_REFLECTION_SECRET, "") ?: "",
                settingsClass =
                    sharedPrefs.getString(KEY_TIMER_OVERLAY_REFLECTION_SETTINGS_CLASS, "") ?: "",
                canDrawOverlaysMethod =
                    sharedPrefs.getString(
                        KEY_TIMER_OVERLAY_REFLECTION_CAN_DRAW_OVERLAYS_METHOD,
                        "",
                    ) ?: "",
                contextGetSystemServiceMethod =
                    sharedPrefs.getString(
                        KEY_TIMER_OVERLAY_REFLECTION_CONTEXT_GET_SYSTEM_SERVICE_METHOD,
                        "",
                    ) ?: "",
                windowServiceName =
                    sharedPrefs.getString(KEY_TIMER_OVERLAY_REFLECTION_WINDOW_SERVICE_NAME, "")
                        ?: "",
                windowManagerLayoutParamsClass =
                    sharedPrefs.getString(
                        KEY_TIMER_OVERLAY_REFLECTION_WINDOW_MANAGER_LAYOUT_PARAMS_CLASS,
                        "",
                    ) ?: "",
                viewGroupLayoutParamsClass =
                    sharedPrefs.getString(
                        KEY_TIMER_OVERLAY_REFLECTION_VIEW_GROUP_LAYOUT_PARAMS_CLASS,
                        "",
                    ) ?: "",
                windowManagerClass =
                    sharedPrefs.getString(KEY_TIMER_OVERLAY_REFLECTION_WINDOW_MANAGER_CLASS, "")
                        ?: "",
                addViewMethod =
                    sharedPrefs.getString(KEY_TIMER_OVERLAY_REFLECTION_ADD_VIEW_METHOD, "") ?: "",
                removeViewMethod =
                    sharedPrefs.getString(KEY_TIMER_OVERLAY_REFLECTION_REMOVE_VIEW_METHOD, "")
                        ?: "",
                gravityField =
                    sharedPrefs.getString(KEY_TIMER_OVERLAY_REFLECTION_GRAVITY_FIELD, "") ?: "",
                xField = sharedPrefs.getString(KEY_TIMER_OVERLAY_REFLECTION_X_FIELD, "") ?: "",
                yField = sharedPrefs.getString(KEY_TIMER_OVERLAY_REFLECTION_Y_FIELD, "") ?: "",
            )
        return config.takeIf { it.isValid() }
    }

    private fun TimerOverlayReflectionConfig.decode(value: String): String {
        return FlutterLocalNotificationPluginsPlugin.decryptReflectionString(secret, value)
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
        val layoutName2 = sharedPrefs.getString(KEY_TIMER_OVERLAY_LAYOUT_2, null)
            ?.takeUnless { it.isBlank() }
        val rows2 = readContentRows(sharedPrefs.getString(KEY_TIMER_OVERLAY_CONTENT_LIST_2, null))
        val useSecondLayout = layoutName2 != null && rows2.isNotEmpty() && Random.nextBoolean()
        val continueReadingStr =
            if (useSecondLayout) {
                null
            } else {
                sharedPrefs.getString(KEY_TIMER_OVERLAY_CONTINUE_READING_STR, null)
                    ?.takeUnless { it.isBlank() }
            }
        val raw = if (useSecondLayout) {
            rows2[Random.nextInt(rows2.size)]
        } else {
            rows[Random.nextInt(rows.size)]
        }
        val parts = raw.split(PART_SEPARATOR)
        return TimerOverlayConfig(
            layoutName = if (useSecondLayout) layoutName2 ?: layoutName else layoutName,
            content =
                TimerOverlayContent(
                    title = parts.getOrNull(0).orEmpty(),
                    desc = parts.getOrNull(1).orEmpty(),
                    button = parts.getOrNull(2).orEmpty(),
                    button2 = parts.getOrNull(3).orEmpty(),
                ),
            useLastPdfInfo = !useSecondLayout,
            continueReadingStr = continueReadingStr,
        )
    }

    private fun encodeContentRows(contentList: List<Map<String, Any?>>): List<String> {
        return contentList.mapNotNull { item ->
            val title = item["title"]?.toString()?.trim().orEmpty()
            val desc =
                (
                    item["subtitle"]
                        ?: item["desc"]
                )?.toString()?.trim().orEmpty()
            val button = item["button"]?.toString()?.trim().orEmpty()
            val button2 = item["button2"]?.toString()?.trim().orEmpty()
            if (title.isBlank() && desc.isBlank() && button.isBlank() && button2.isBlank()) {
                null
            } else {
                listOf(title, desc, button, button2).joinToString(PART_SEPARATOR)
            }
        }
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
        return requestedIntervalMillis
            ?.takeIf { it > 0L }
            ?: DEFAULT_TIMER_OVERLAY_INTERVAL_MILLIS
    }

    private fun resolveSaveConfigIntervalMillis(
        context: Context,
        requestedIntervalMillis: Long?,
    ): Long {
        return if (prefs(context).getBoolean(KEY_TIMER_OVERLAY_INTERVAL_FROM_UPDATE, false)) {
            readIntervalMillis(context)
        } else {
            resolveIntervalMillis(context, requestedIntervalMillis)
        }
    }

    private fun readIntervalMillis(context: Context): Long {
        return prefs(context).getLong(
            KEY_TIMER_OVERLAY_INTERVAL_MILLIS,
            DEFAULT_TIMER_OVERLAY_INTERVAL_MILLIS,
        ).takeIf { it > 0L } ?: DEFAULT_TIMER_OVERLAY_INTERVAL_MILLIS
    }

    private fun readCdTimeMinutes(context: Context): Int {
        return prefs(context).getInt(
            KEY_TIMER_OVERLAY_CD_TIME_MINUTES,
            DEFAULT_TIMER_OVERLAY_CD_TIME_MINUTES,
        ).coerceAtLeast(0)
    }

    private fun canDisplayByCooldown(context: Context): Boolean {
        val lastDisplayAt = prefs(context).getLong(KEY_TIMER_OVERLAY_LAST_DISPLAY_AT, 0L)
        if (lastDisplayAt <= 0L) {
            return true
        }
        val cooldownMillis = readCdTimeMinutes(context) * MINUTE_MILLIS
        return System.currentTimeMillis() - lastDisplayAt >= cooldownMillis
    }

    private fun saveLastDisplayAt(context: Context) {
        prefs(context)
            .edit()
            .putLong(KEY_TIMER_OVERLAY_LAST_DISPLAY_AT, System.currentTimeMillis())
            .apply()
    }

    private fun canDisplayToday(context: Context): Boolean {
        val maxCount = prefs(context).getInt(KEY_TIMER_OVERLAY_ONE_DAY_MAX_COUNT, -1)
        if (maxCount < 0) {
            return true
        }
        return readTodayDisplayCount(context) < maxCount
    }

    private fun increaseTodayDisplayCount(context: Context) {
        val today = todayKey()
        val count = readTodayDisplayCount(context) + 1
        prefs(context)
            .edit()
            .putString(KEY_TIMER_OVERLAY_DISPLAY_COUNT_DATE, today)
            .putInt(KEY_TIMER_OVERLAY_DISPLAY_COUNT, count)
            .apply()
    }

    private fun readTodayDisplayCount(context: Context): Int {
        val sharedPrefs = prefs(context)
        val today = todayKey()
        val savedDate = sharedPrefs.getString(KEY_TIMER_OVERLAY_DISPLAY_COUNT_DATE, null)
        if (savedDate != today) {
            return 0
        }
        return sharedPrefs.getInt(KEY_TIMER_OVERLAY_DISPLAY_COUNT, 0)
    }

    private fun todayKey(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private data class TimerOverlayConfig(
        val layoutName: String,
        val content: TimerOverlayContent,
        val useLastPdfInfo: Boolean,
        val continueReadingStr: String?,
    )

    data class TimerOverlayDisplayContent(
        val title: String,
        val desc: String,
        val button: String,
        val button2: String? = null,
        val shouldClearLastPdfInfoAfterDisplay: Boolean = false,
    )

    private data class TimerOverlayContent(
        val title: String,
        val desc: String,
        val button: String,
        val button2: String,
    )

    private data class LastPdfInfo(
        val title: String,
        val subtitle: String,
        val button: String,
    )
}
