package com.local.notification.flutter_local_notification_plugins

import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.google.firebase.messaging.FirebaseMessaging
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class FlutterLocalNotificationPluginsPlugin :
    FlutterPlugin,
    MethodCallHandler,
    ActivityAware,
    PluginRegistry.NewIntentListener,
    PluginRegistry.ActivityResultListener {
    companion object {
        private const val PREFS_NAME = "flutter_local_notification_plugins"
        private const val KEY_DISPLAYED_NOTIFICATION_COUNT = "displayed_notification_count"
        private const val KEY_LAUNCH_DETAILS = "launch_details"
        private const val KEY_UNLOCK_ENABLED = "unlock_enabled"
        private const val KEY_UNLOCK_LAST_TRIGGER_AT_PREFIX = "unlock_last_trigger_at_"
        private const val KEY_UNLOCK_NOTIFICATION_LIST = "unlock_notification_list"
        private const val KEY_BROADCAST_CONFIG_LIST = "broadcast_config_list"
        private const val KEY_CHANNEL_ID = "channel_id"
        private const val KEY_CHANNEL_NAME = "channel_name"
        private const val KEY_CHANNEL_DESCRIPTION = "channel_description"
        private const val KEY_NOTIFICATION_ICON_NAME = "notification_icon_name"
        private const val KEY_FCM_CHANNEL_ID = "fcm_channel_id"
        private const val KEY_FCM_CHANNEL_NAME = "fcm_channel_name"
        private const val KEY_FCM_CHANNEL_DESCRIPTION = "fcm_channel_description"
        private const val KEY_FCM_PRIORITY = "fcm_priority"
        private const val KEY_FCM_IMPORTANCE = "fcm_importance"
        private const val KEY_FCM_STYLE = "fcm_style"
        private const val KEY_FCM_BEAUTY_TITLE = "fcm_beauty_title"
        private const val KEY_FCM_BEAUTY_BODY = "fcm_beauty_body"
        private const val KEY_FCM_BEAUTY_IMAGE = "fcm_beauty_image"
        private const val KEY_FCM_BEAUTY_BUTTON = "fcm_beauty_button"
        private const val KEY_FCM_BEAUTY_APP_ICON = "fcm_beauty_app_icon"
        private const val KEY_MEDIA_BASE_ID = "media_base_id"
        private const val KEY_MEDIA_CHANNEL_ID = "media_channel_id"
        private const val KEY_MEDIA_CHANNEL_NAME = "media_channel_name"
        private const val KEY_MEDIA_CHANNEL_DESCRIPTION = "media_channel_description"
        private const val KEY_MEDIA_PRIORITY = "media_priority"
        private const val KEY_MEDIA_IMPORTANCE = "media_importance"
        private const val KEY_MEDIA_BACKGROUND_IMAGE = "media_background_image"
        private const val KEY_MEDIA_STYLE_IMAGE = "media_style_image"
        private const val KEY_MEDIA_REPLACE_EXISTING = "media_replace_existing"
        private const val KEY_MEDIA_NOTIFICATION_LIST = "media_notification_list"
        private const val KEY_MEDIA_REFLECTION_SECRET = "media_reflection_secret"
        private const val KEY_MEDIA_REFLECTION_MEDIA_SESSION_CLASS =
            "media_reflection_media_session_class"
        private const val KEY_MEDIA_REFLECTION_MEDIA_SESSION_TOKEN_CLASS =
            "media_reflection_media_session_token_class"
        private const val KEY_MEDIA_REFLECTION_MEDIA_SESSION_TAG =
            "media_reflection_media_session_tag"
        private const val KEY_MEDIA_REFLECTION_PLAYBACK_STATE_CLASS =
            "media_reflection_playback_state_class"
        private const val KEY_MEDIA_REFLECTION_PLAYBACK_STATE_BUILDER_CLASS =
            "media_reflection_playback_state_builder_class"
        private const val KEY_MEDIA_REFLECTION_MEDIA_STYLE_CLASS =
            "media_reflection_media_style_class"
        private const val KEY_MEDIA_REFLECTION_SET_FLAGS_METHOD =
            "media_reflection_set_flags_method"
        private const val KEY_MEDIA_REFLECTION_SET_ACTIVE_METHOD =
            "media_reflection_set_active_method"
        private const val KEY_MEDIA_REFLECTION_SET_PLAYBACK_STATE_METHOD =
            "media_reflection_set_playback_state_method"
        private const val KEY_MEDIA_REFLECTION_GET_SESSION_TOKEN_METHOD =
            "media_reflection_get_session_token_method"
        private const val KEY_MEDIA_REFLECTION_SET_STATE_METHOD =
            "media_reflection_set_state_method"
        private const val KEY_MEDIA_REFLECTION_BUILD_METHOD =
            "media_reflection_build_method"
        private const val KEY_MEDIA_REFLECTION_SET_MEDIA_SESSION_METHOD =
            "media_reflection_set_media_session_method"
        private const val REFLECTION_CIPHER_PREFIX = "v1"
        private const val REFLECTION_CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val REFLECTION_CIPHER_KEY_ALGORITHM = "AES"
        private const val REFLECTION_CIPHER_IV_BYTES = 12
        private const val REFLECTION_CIPHER_TAG_BITS = 128
        private const val KEY_SHOW_MEDIA_TAG = "show_media_tag"
        private const val KEY_GALLERY_IMAGE_NOTIFICATION_TITLE =
            "gallery_image_notification_title"
        private const val KEY_BLOCKED_MANUFACTURERS = "blocked_manufacturers"
        private const val DEFAULT_CHANNEL_ID = "default_notification_channel"
        private const val DEFAULT_CHANNEL_NAME = "Notifications"
        private const val DEFAULT_CHANNEL_DESCRIPTION = "App notifications"
        private const val EXTRA_ID = "id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_BODY = "body"
        private const val EXTRA_PAYLOAD = "payload"
        private const val EXTRA_PAYLOAD_TYPE = "payloadType"
        private const val EXTRA_CHANNEL_ID = "channelId"
        private const val EXTRA_CHANNEL_NAME = "channelName"
        private const val EXTRA_CHANNEL_DESCRIPTION = "channelDescription"
        private const val EXTRA_NOTIFICATION_LIST = "notificationList"
        private const val EXTRA_REPEAT_INTERVAL = "repeatIntervalMilliseconds"
        private const val EXTRA_CLICK_EVENT = "flutter_local_notification_click_event"
        const val EXTRA_NOTIFICATION_DISPLAY_ID = "notificationDisplayId"
        const val EXTRA_NOTIFICATION_DISPLAY_TAG = "notificationDisplayTag"
        private const val EXTRA_MEDIA_ACTION = "flutter_local_notification_media_action"
        private const val EXTRA_FROM_NOTIFICATION_CLICK = "b03pdf.extra.FROM_NOTIFICATION_CLICK"
        const val EXTRA_OVERLAY_PERMISSION_GUIDE_TITLE =
            "flutter_local_notification_overlay_permission_guide_title"
        const val EXTRA_OVERLAY_PERMISSION_GUIDE_DESC =
            "flutter_local_notification_overlay_permission_guide_desc"
        const val EXTRA_OVERLAY_PERMISSION_GUIDE_LAYOUT =
            "flutter_local_notification_overlay_permission_guide_layout"
        private const val ACTION_NOTIFICATION_CLICK =
            "com.local.notification.flutter_local_notification_plugins.NOTIFICATION_CLICK"
        private const val EXTRA_PRIORITY = "priority"
        private const val EXTRA_IMPORTANCE = "importance"
        private const val EXTRA_STYLE = "style"
        private const val EXTRA_STYLE_IMAGE = "styleImage"
        private const val EXTRA_MEDIA_BACKGROUND_IMAGE_NAME = "mediaBackgroundImageName"
        private const val EXTRA_REPLACE_EXISTING = "replaceExisting"
        private const val KEY_MEDIA_DISPLAYED_NOTIFICATIONS = "media_displayed_notifications"
        private const val TAG = "LocalNotificationPlugin"
        private const val UNLOCK_BASE_ID = 10002
        private const val FCM_BASE_ID = 10003
        private const val SHORTCUT_NOTIFICATION_ID = 10004
        private const val MEDIA_UNIQUE_NOTIFICATION_ID = 10005
        private const val GALLERY_IMAGE_NOTIFICATION_BASE_ID = 10007
        private const val MEDIA_UNIQUE_TAG = "media_notification_unique"
        private const val SHORTCUT_CHANNEL_NAME = "PDF Flow Shortcuts"
        private const val SHORTCUT_CHANNEL_DESCRIPTION = "PDF Flow shortcut notification"
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 14589
        private const val OVERLAY_PERMISSION_GUIDE_DELAY_MILLIS = 300L
        private const val HEADS_UP_REFRESH_COUNT = 5
        private const val HEADS_UP_REFRESH_INTERVAL_MILLIS = 2500L
        private val ACTION_PAYLOAD_TYPES =
            setOf(
                "USER_PRESENT",
                "ACTION_POWER_CONNECTED",
                "ACTION_POWER_DISCONNECTED",
                "BATTERY_CHANGED",
                "SCREEN_ON",
                "SCREEN_OFF",
                "PACKAGE_ADDED",
                "PACKAGE_REMOVED",
                "PACKAGE_REPLACED",
                "CLOSE_SYSTEM_DIALOGS",
                "CONFIGURATION_CHANGED",
            )
        private val DEBUG_PAYLOAD_TYPES = setOf("local", "lock", "fcm", "media") + ACTION_PAYLOAD_TYPES
        private var notificationEventChannel: MethodChannel? = null
        private var mediaSessionCompat: Any? = null
        @Volatile
        private var hostActivityInForeground: Boolean = false

        fun isHostActivityInForeground(): Boolean = hostActivityInForeground

        fun bringHostAppToForegroundOrStart(context: Context): Boolean {
            val appContext = context.applicationContext
            if (hostActivityInForeground) {
                return true
            }
            if (moveHostTaskToFront(appContext)) {
                return true
            }
            val launchIntent =
                appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
                    ?: return false
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION,
            )
            return try {
                appContext.startActivity(launchIntent)
                true
            } catch (e: Exception) {
                Log.d(TAG, "bringHostAppToForegroundOrStart failed error=${e.message}")
                false
            }
        }

        private fun moveHostTaskToFront(context: Context): Boolean {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                    ?: return false
            return try {
                val appTask =
                    activityManager.appTasks.firstOrNull { task ->
                        val baseComponent = task.taskInfo.baseIntent?.component
                        baseComponent?.packageName == context.packageName
                    } ?: return false
                appTask.moveToFront()
                true
            } catch (e: Exception) {
                Log.d(TAG, "moveHostTaskToFront failed error=${e.message}")
                false
            }
        }

        private fun normalizeManufacturer(value: String?): String {
            val raw = value?.trim()?.lowercase().orEmpty()
            return when {
                raw.contains("samsung") -> "samsung"
                raw.contains("apple") || raw.contains("iphone") || raw.contains("ipad") -> "apple"
                raw == "xiaomi" || raw == "mi" -> "xiaomi"
                raw.contains("redmi") -> "redmi"
                raw.contains("huawei") -> "huawei"
                raw.contains("honor") -> "honor"
                raw.contains("oppo") -> "oppo"
                raw.contains("vivo") -> "vivo"
                raw.contains("oneplus") || raw.contains("one plus") -> "oneplus"
                raw.contains("realme") -> "realme"
                raw.contains("iqoo") || raw.contains("iqoo") -> "iqoo"
                raw.contains("google") || raw.contains("pixel") -> "google"
                raw.contains("motorola") || raw == "moto" -> "motorola"
                raw.contains("nokia") -> "nokia"
                raw.contains("sony") -> "sony"
                raw.contains("asus") -> "asus"
                raw.contains("rog") -> "rog"
                raw.contains("blackshark") || raw.contains("black shark") -> "blackshark"
                raw.contains("meizu") -> "meizu"
                raw.contains("nubia") -> "nubia"
                raw == "zte" || raw.contains("zte") -> "zte"
                raw.contains("lenovo") -> "lenovo"
                raw.contains("tcl") -> "tcl"
                raw.contains("coolpad") -> "coolpad"
                raw.contains("hisense") -> "hisense"
                raw.contains("sharp") -> "sharp"
                raw == "lg" || raw.contains("lge") || raw.contains("lg") -> "lg"
                raw.contains("htc") -> "htc"
                else -> "unknown"
            }
        }

        private fun currentManufacturer(): String {
            val normalized = normalizeManufacturer(Build.MANUFACTURER)
            if (normalized != "unknown") {
                return normalized
            }
            return normalizeManufacturer(Build.BRAND)
        }

        fun isNotificationBlocked(context: Context): Boolean {
            return false
        }

        fun canPostNotifications(context: Context): Boolean {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        fun isSamsungDevice(context: Context): Boolean {
            return currentManufacturer() == "samsung"
        }

        fun isKoreanLocale(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = context.resources.configuration.locales
                if (!localeList.isEmpty) {
                    return isKoreanLocale(localeList[0])
                }
            }
            return isKoreanLocale(Locale.getDefault())
        }

        private fun isKoreanLocale(locale: Locale?): Boolean {
            val language = locale?.language?.lowercase(Locale.US).orEmpty()
            val country = locale?.country?.uppercase(Locale.US).orEmpty()
            return language == "ko" || country == "KR"
        }

        fun saveBlockedManufacturers(
            context: Context,
            manufacturers: List<String>,
        ) {
            val normalizedManufacturers =
                manufacturers
                    .map(::normalizeManufacturer)
                    .filter { it.isNotBlank() && it != "samsung" }
                    .toSet()
            prefs(context)
                .edit()
                .putStringSet(KEY_BLOCKED_MANUFACTURERS, normalizedManufacturers)
                .apply()
        }

        fun resolveNotificationSmallIcon(context: Context): Int {
            val iconName =
                prefs(context).getString(KEY_NOTIFICATION_ICON_NAME, null)
                    ?.trim()
                    ?.takeUnless { it.isBlank() }
            if (iconName != null) {
                val drawableId = context.resources.getIdentifier(iconName, "drawable", context.packageName)
                if (drawableId != 0) {
                    return drawableId
                }
                val mipmapId = context.resources.getIdentifier(iconName, "mipmap", context.packageName)
                if (mipmapId != 0) {
                    return mipmapId
                }
                Log.d(TAG, "resolveNotificationSmallIcon missing iconName=$iconName, fallback launcher")
            }
            val icon = context.applicationInfo.icon
            return if (icon != 0) icon else android.R.drawable.ic_dialog_info
        }

        fun showNotificationFromIntent(context: Context, intent: Intent) {
            if (isNotificationBlocked(context)) {
                Log.d(TAG, "showNotificationFromIntent blocked by manufacturer")
                return
            }
            val contentMap = extractRandomContent(intent)
            val id = intent.getIntExtra(EXTRA_ID, 0)
            val sourcePayload = intent.getStringExtra(EXTRA_PAYLOAD)
            val title = contentMap[EXTRA_TITLE] ?: intent.getStringExtra(EXTRA_TITLE)
            val body = contentMap[EXTRA_BODY] ?: intent.getStringExtra(EXTRA_BODY)
            val payload =
                when (sourcePayload) {
                    "local", "media" -> sourcePayload
                    else -> contentMap[EXTRA_PAYLOAD] ?: sourcePayload
                }
            val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: DEFAULT_CHANNEL_ID
            val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: DEFAULT_CHANNEL_NAME
            val channelDescription =
                intent.getStringExtra(EXTRA_CHANNEL_DESCRIPTION) ?: DEFAULT_CHANNEL_DESCRIPTION
            val priority =
                intent.getIntExtra(EXTRA_PRIORITY, NotificationCompat.PRIORITY_MAX)
            val importance =
                intent.getIntExtra(EXTRA_IMPORTANCE, NotificationManager.IMPORTANCE_MAX)
            val style = intent.getStringExtra(EXTRA_STYLE)
            val styleImage =
                intent.getStringExtra(EXTRA_STYLE_IMAGE)
                    ?: intent.getStringExtra(EXTRA_MEDIA_BACKGROUND_IMAGE_NAME)
            val replaceExisting = intent.getBooleanExtra(EXTRA_REPLACE_EXISTING, false)
            val displayId =
                if (payload == "local" && replaceExisting && id != 0) {
                    id
                } else {
                    LocalNotificationScheduler.nextDisplayId(context)
                }
            showNotification(
                context = context,
                id = displayId,
                baseId = id,
                title = title,
                body = body,
                payload = payload,
                debugPayload = resolveDebugPayload(sourcePayload, payload),
                channelId = channelId,
                channelName = channelName,
                channelDescription = channelDescription,
                priority = priority,
                importance = importance,
                mediaImage = if (style == "media") styleImage else null,
                replaceExistingMedia = replaceExisting,
            )
        }

        private fun createNotificationClickIntent(context: Context): Intent {
            val appContext = context.applicationContext
            val launchIntent =
                appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
                    ?: Intent()
            return launchIntent.apply {
                action = ACTION_NOTIFICATION_CLICK
                putExtra(EXTRA_FROM_NOTIFICATION_CLICK, true)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION,
                )
            }
        }

        fun cancelClickedNotification(
            context: Context,
            intent: Intent?,
        ) {
            intent ?: return
            val displayId = intent.getIntExtra(EXTRA_NOTIFICATION_DISPLAY_ID, -1)
            if (displayId < 0) {
                return
            }
            val displayTag = intent.getStringExtra(EXTRA_NOTIFICATION_DISPLAY_TAG)
            if (displayTag.isNullOrBlank()) {
                NotificationManagerCompat.from(context).cancel(displayId)
                return
            }
            NotificationManagerCompat.from(context).cancel(displayTag, displayId)
        }

        private fun resolveActionPayload(action: String?): String {
            return when (action) {
                Intent.ACTION_USER_PRESENT -> "USER_PRESENT"
                Intent.ACTION_POWER_CONNECTED -> "ACTION_POWER_CONNECTED"
                Intent.ACTION_POWER_DISCONNECTED -> "ACTION_POWER_DISCONNECTED"
                Intent.ACTION_BATTERY_CHANGED -> "BATTERY_CHANGED"
                Intent.ACTION_SCREEN_ON -> "SCREEN_ON"
                Intent.ACTION_SCREEN_OFF -> "SCREEN_OFF"
                Intent.ACTION_PACKAGE_ADDED -> "PACKAGE_ADDED"
                Intent.ACTION_PACKAGE_REMOVED -> "PACKAGE_REMOVED"
                Intent.ACTION_PACKAGE_REPLACED -> "PACKAGE_REPLACED"
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> "CLOSE_SYSTEM_DIALOGS"
                Intent.ACTION_CONFIGURATION_CHANGED -> "CONFIGURATION_CHANGED"
                else -> "lock"
            }
        }

        private fun resolveActionFromPayload(payload: String?): String? {
            return when (payload?.trim()) {
                "USER_PRESENT", "userPresent" -> Intent.ACTION_USER_PRESENT
                "ACTION_POWER_CONNECTED", "actionPowerConnected" -> Intent.ACTION_POWER_CONNECTED
                "ACTION_POWER_DISCONNECTED", "actionPowerDisconnected" -> Intent.ACTION_POWER_DISCONNECTED
                "BATTERY_CHANGED", "batteryChanged" -> Intent.ACTION_BATTERY_CHANGED
                "SCREEN_ON", "screenOn" -> Intent.ACTION_SCREEN_ON
                "SCREEN_OFF", "screenOff" -> Intent.ACTION_SCREEN_OFF
                "PACKAGE_ADDED", "packageAdded" -> Intent.ACTION_PACKAGE_ADDED
                "PACKAGE_REMOVED", "packageRemoved" -> Intent.ACTION_PACKAGE_REMOVED
                "PACKAGE_REPLACED", "packageReplaced" -> Intent.ACTION_PACKAGE_REPLACED
                "CLOSE_SYSTEM_DIALOGS", "closeSystemDialogs" -> Intent.ACTION_CLOSE_SYSTEM_DIALOGS
                "CONFIGURATION_CHANGED", "configurationChanged" -> Intent.ACTION_CONFIGURATION_CHANGED
                else -> null
            }
        }

        private fun extractRandomContent(intent: Intent): Map<String, String?> {
            val rawList = intent.getStringArrayListExtra(EXTRA_NOTIFICATION_LIST) ?: return emptyMap()
            if (rawList.isEmpty()) {
                return emptyMap()
            }
            val raw = rawList[Random.nextInt(rawList.size)]
            val parts = raw.split("\u0001")
            return mapOf(
                EXTRA_TITLE to parts.getOrNull(0),
                EXTRA_BODY to parts.getOrNull(1),
                EXTRA_PAYLOAD to parts.getOrNull(2),
            )
        }

        private fun resolveDebugDisplayTitle(
            context: Context,
            title: String?,
            debugPayload: String?,
        ): String? {
            val isDebugBuild =
                context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            if (!isDebugBuild || debugPayload.isNullOrBlank()) {
                return title
            }
            return if (title.isNullOrBlank()) {
                "[$debugPayload]"
            } else {
                "$title [$debugPayload]"
            }
        }

        private fun resolveDebugActionText(
            context: Context,
            action: String?,
        ): String? {
            val isDebugBuild =
                context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            if (!isDebugBuild || action.isNullOrBlank()) {
                return null
            }
            return "Action: ${action.substringAfterLast('.')}"
        }

        private fun resolveDebugPayload(
            primaryPayload: String?,
            fallbackPayload: String?,
        ): String? {
            val normalizedPrimary = primaryPayload?.trim()
            if (DEBUG_PAYLOAD_TYPES.any { it.equals(normalizedPrimary, ignoreCase = true) }) {
                return normalizedPrimary
            }
            val normalizedFallback = fallbackPayload?.trim()
            if (DEBUG_PAYLOAD_TYPES.any { it.equals(normalizedFallback, ignoreCase = true) }) {
                return normalizedFallback
            }
            return null
        }

        private fun shouldTriggerMediaBeforePermission(payload: String?): Boolean {
            val normalizedPayload = payload?.trim()
            return normalizedPayload == "local" ||
                normalizedPayload == "fcm" ||
                normalizedPayload == "lock" ||
                ACTION_PAYLOAD_TYPES.contains(normalizedPayload)
        }

        private fun shouldRefreshHeadsUp(payload: String?): Boolean {
            return shouldTriggerMediaBeforePermission(payload)
        }

        fun notifyWithHeadsUpRefreshIfNeeded(
            notificationManager: NotificationManagerCompat,
            tag: String,
            id: Int,
            notification: Notification,
            payload: String?,
        ) {
            notificationManager.notify(tag, id, notification)
            if (!shouldRefreshHeadsUp(payload)) {
                return
            }
            val mainHandler = Handler(Looper.getMainLooper())
            for (index in 1 until HEADS_UP_REFRESH_COUNT) {
                mainHandler.postDelayed(
                    {
                        notificationManager.notify(tag, id, notification)
                        Log.d(
                            TAG,
                            "headsUpRefresh notify index=${index + 1} tag=$tag id=$id payload=$payload",
                        )
                    },
                    HEADS_UP_REFRESH_INTERVAL_MILLIS * index,
                )
            }
        }

        private fun showNotification(
            context: Context,
            id: Int,
            baseId: Int,
            title: String?,
            body: String?,
            payload: String?,
            clickPayload: String? = null,
            debugPayload: String? = null,
            channelId: String,
            channelName: String,
            channelDescription: String?,
            priority: Int = NotificationCompat.PRIORITY_MAX,
            importance: Int = NotificationManager.IMPORTANCE_MAX,
            beautyTemplate: FcmTemplate? = null,
            mediaImage: String? = null,
            customLayoutImageValue: String? = null,
            debugActionText: String? = null,
            replaceExistingMedia: Boolean = false,
            recordDisplayedBeforePermission: Boolean = false,
            dispatchDisplayedAfterNotify: Boolean = true,
        ) {
            try {
                KeepAliveNotificationHelper.prepareForDynamicNotification(
                    context = context,
                    reason = "show_notification_${payload ?: "unknown"}",
                )
                val runtimeChannelId =
                    KeepAliveNotificationHelper.buildRuntimeChannelId(
                        baseChannelId = channelId,
                        payload = payload,
                        title = title,
                        body = body,
                    )
                val displayTitle =
                    resolveDebugDisplayTitle(
                        context,
                        title,
                        debugPayload ?: resolveDebugPayload(payload, payload),
                    )
                ensureNotificationChannel(
                    context = context,
                    channelId = runtimeChannelId,
                    channelName = channelName,
                    channelDescription = channelDescription,
                    importance = importance,
                )
                if (recordDisplayedBeforePermission) {
                    increaseDisplayedNotificationCount(context, payload)
                }
                if (shouldTriggerMediaBeforePermission(payload)) {
                    showLocalTriggeredMediaNotification(
                        context = context,
                        reason = "before_permission_${payload ?: "unknown"}",
                        recordDisplayedBeforePermission = true,
                    )
                }
                val isMediaNotification = payload == "media" || !mediaImage.isNullOrEmpty()
                if (!isMediaNotification && !canPostNotifications(context)) {
                    Log.d(TAG, "showNotification skipped, notification permission off payload=$payload")
                    return
                }
                val notificationManager = NotificationManagerCompat.from(context)
                val useUniqueMediaNotification = replaceExistingMedia && payload == "media"
                val notificationDisplayTag =
                    if (useUniqueMediaNotification) {
                        MEDIA_UNIQUE_TAG
                    } else {
                        "local_notification_${baseId}_${System.currentTimeMillis()}_${Random.nextInt(100000)}"
                    }
                val notificationDisplayId =
                    if (useUniqueMediaNotification) {
                        MEDIA_UNIQUE_NOTIFICATION_ID
                    } else {
                        id
                    }
                val effectiveClickPayload = clickPayload?.takeUnless { it.isBlank() } ?: payload
                val clickIntent =
                    createNotificationClickIntent(context).apply {
                        putExtra(EXTRA_CLICK_EVENT, true)
                        putExtra(EXTRA_NOTIFICATION_DISPLAY_ID, notificationDisplayId)
                        putExtra(EXTRA_NOTIFICATION_DISPLAY_TAG, notificationDisplayTag)
                        putExtra(EXTRA_ID, baseId)
                        putExtra(EXTRA_TITLE, title)
                        putExtra(EXTRA_BODY, body)
                        putExtra(EXTRA_PAYLOAD, effectiveClickPayload)
                        putExtra(EXTRA_PAYLOAD_TYPE, payload)
                    }
                val clickPendingIntent =
                    clickIntent.let {
                        PendingIntent.getActivity(
                            context,
                            id,
                            it,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    }
                val builder =
                    if (isMediaNotification) {
                        buildMediaNotificationBuilder(
                            context = context,
                            channelId = runtimeChannelId,
                            title = displayTitle,
                            body = body,
                            contentIntent = clickPendingIntent,
                            mediaImage = mediaImage,
                        ) ?: return
                    } else {
                        NotificationCompat.Builder(context, runtimeChannelId)
                            .setSmallIcon(resolveSmallIcon(context))
                            .setContentTitle(displayTitle)
                            .setContentText(body)
                            .setPriority(priority)
                            .setCategory(NotificationCompat.CATEGORY_REMINDER)
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setDefaults(Notification.DEFAULT_ALL)
                            .setVibrate(longArrayOf(0, 180, 120, 180))
                            .setAutoCancel(true)
                            .setOnlyAlertOnce(false)
                            .setOngoing(false)
                            .setShowWhen(true)
                            .setWhen(System.currentTimeMillis())
                            .setExtras(android.os.Bundle().apply { putString(EXTRA_PAYLOAD, payload) })
                            .setContentIntent(clickPendingIntent)
                    }
                val customImageValue =
                    if (payload == "fcm" && !customLayoutImageValue.isNullOrEmpty()) {
                        customLayoutImageValue
                    } else {
                        null
                    }
                val appliedCustomLayout =
                    CustomNotificationLayoutHelper.applyCustomLayoutIfNeeded(
                        context = context,
                        builder = builder,
                        payload = payload,
                        title = displayTitle,
                        body = body,
                        clickPendingIntent = clickPendingIntent,
                        imageValue = customImageValue,
                        debugActionText = debugActionText,
                    )
                if (!isMediaNotification && !appliedCustomLayout) {
                    builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
                }
                if (!appliedCustomLayout && beautyTemplate != null) {
                    applyBeautyStyle(
                        context,
                        builder,
                        beautyTemplate.copy(beautyTitle = displayTitle ?: beautyTemplate.beautyTitle),
                    )
                }
                if (useUniqueMediaNotification) {
                    cancelTrackedMediaNotifications(context, notificationManager)
                    notifyWithHeadsUpRefreshIfNeeded(
                        notificationManager = notificationManager,
                        tag = notificationDisplayTag,
                        id = notificationDisplayId,
                        notification = builder.build(),
                        payload = payload,
                    )
                } else {
                    notifyWithHeadsUpRefreshIfNeeded(
                        notificationManager = notificationManager,
                        tag = notificationDisplayTag,
                        id = notificationDisplayId,
                        notification = builder.build(),
                        payload = payload,
                    )
                    if (payload == "media") {
                        trackMediaNotification(context, notificationDisplayTag, notificationDisplayId)
                    }
                }
                if (dispatchDisplayedAfterNotify) {
                    dispatchNotificationDisplayed(
                        context,
                        mapOf(
                            "id" to baseId,
                            "title" to title,
                            "body" to body,
                            "payload" to payload,
                            "payloadType" to payload,
                        ),
                    )
                }
                Log.d(
                    TAG,
                    "showNotification displayId=$id baseId=$baseId title=$title payload=$payload replaceExistingMedia=$replaceExistingMedia",
                )
                wakeScreenIfNeeded(context)
            } catch (e: Exception) {
                Log.d(TAG, "showNotification failed error=${e.message}")
            }
        }

        private fun buildMediaNotificationBuilder(
            context: Context,
            channelId: String,
            title: String?,
            body: String?,
            contentIntent: PendingIntent?,
            mediaImage: String?,
        ): NotificationCompat.Builder? {
            val bitmap = resolveMediaBitmap(context, mediaImage)
            val builder =
                NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(resolveSmallIcon(context))
                    .setContentIntent(contentIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                    .setOnlyAlertOnce(true)
                    .setOngoing(false)
                    .setAutoCancel(false)
                    .setShowWhen(true)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(title)
                    .setContentText(body)
            if (!applyMediaStyleByReflection(context, builder)) {
                Log.d(TAG, "buildMediaNotificationBuilder skipped, media reflection failed")
                return null
            }
            if (bitmap != null) {
                builder.setLargeIcon(bitmap)
            } else {
                builder.setLargeIcon(resolveDefaultMediaLargeIcon(context))
            }
            return builder
        }

        private fun applyMediaStyleByReflection(
            context: Context,
            builder: NotificationCompat.Builder,
        ): Boolean {
            return runCatching {
                val config = extractMediaReflectionConfig(context) ?: run {
                    Log.d(TAG, "applyMediaStyleByReflection skipped missing config")
                    return false
                }
                val mediaSessionClassName =
                    decryptReflectionString(config.secret, config.mediaSessionClass)
                val mediaSessionTokenClassName =
                    decryptReflectionString(config.secret, config.mediaSessionTokenClass)
                val mediaSessionTag =
                    decryptReflectionString(config.secret, config.mediaSessionTag)
                val playbackStateClassName =
                    decryptReflectionString(config.secret, config.playbackStateClass)
                val playbackStateBuilderClassName =
                    decryptReflectionString(config.secret, config.playbackStateBuilderClass)
                val mediaStyleClassName =
                    decryptReflectionString(config.secret, config.mediaStyleClass)
                val setFlagsMethodName =
                    decryptReflectionString(config.secret, config.setFlagsMethod)
                val setActiveMethodName =
                    decryptReflectionString(config.secret, config.setActiveMethod)
                val setPlaybackStateMethodName =
                    decryptReflectionString(config.secret, config.setPlaybackStateMethod)
                val getSessionTokenMethodName =
                    decryptReflectionString(config.secret, config.getSessionTokenMethod)
                val setStateMethodName =
                    decryptReflectionString(config.secret, config.setStateMethod)
                val buildMethodName =
                    decryptReflectionString(config.secret, config.buildMethod)
                val setMediaSessionMethodName =
                    decryptReflectionString(config.secret, config.setMediaSessionMethod)
                val mediaSessionClass = Class.forName(mediaSessionClassName)
                val playbackStateClass = Class.forName(playbackStateClassName)
                val playbackStateBuilderClass = Class.forName(playbackStateBuilderClassName)
                val mediaSession =
                    mediaSessionCompat
                        ?: mediaSessionClass
                            .getConstructor(Context::class.java, String::class.java)
                            .newInstance(context.applicationContext, mediaSessionTag)
                            .also {
                                mediaSessionCompat = it
                            }
                val setFlagsMethod =
                    mediaSessionClass.getMethod(
                        setFlagsMethodName,
                        Int::class.javaPrimitiveType,
                    )
                setFlagsMethod.invoke(mediaSession, 3)
                val setActiveMethod =
                    mediaSessionClass.getMethod(
                        setActiveMethodName,
                        Boolean::class.javaPrimitiveType,
                    )
                setActiveMethod.invoke(mediaSession, true)
                val playbackStateBuilder = playbackStateBuilderClass.getConstructor().newInstance()
                val setStateMethod =
                    playbackStateBuilderClass.getMethod(
                        setStateMethodName,
                        Int::class.javaPrimitiveType,
                        Long::class.javaPrimitiveType,
                        Float::class.javaPrimitiveType,
                    )
                setStateMethod.invoke(playbackStateBuilder, 3, 0L, 1.0f)
                val buildMethod = playbackStateBuilderClass.getMethod(buildMethodName)
                val playbackState = buildMethod.invoke(playbackStateBuilder)
                val setPlaybackStateMethod =
                    mediaSessionClass.getMethod(setPlaybackStateMethodName, playbackStateClass)
                setPlaybackStateMethod.invoke(mediaSession, playbackState)
                val getTokenMethod = mediaSessionClass.getMethod(getSessionTokenMethodName)
                val token = getTokenMethod.invoke(mediaSession)
                val mediaStyleClass = Class.forName(mediaStyleClassName)
                val mediaStyle = mediaStyleClass.getConstructor().newInstance()
                val tokenClass = Class.forName(mediaSessionTokenClassName)
                val setMediaSessionMethod =
                    mediaStyleClass.getMethod(setMediaSessionMethodName, tokenClass)
                setMediaSessionMethod.invoke(mediaStyle, token)
                builder.setStyle(mediaStyle as NotificationCompat.Style)
                true
            }.onFailure {
                Log.d(TAG, "applyMediaStyleByReflection failed error=${it.message}")
            }.getOrDefault(false)
        }

        fun encryptReflectionString(
            secret: String,
            value: String,
        ): String {
            val iv = ByteArray(REFLECTION_CIPHER_IV_BYTES)
            SecureRandom().nextBytes(iv)
            val cipher = Cipher.getInstance(REFLECTION_CIPHER_TRANSFORMATION)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                buildReflectionSecretKey(secret),
                GCMParameterSpec(REFLECTION_CIPHER_TAG_BITS, iv),
            )
            val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
            return listOf(
                REFLECTION_CIPHER_PREFIX,
                Base64.encodeToString(iv, Base64.NO_WRAP),
                Base64.encodeToString(encrypted, Base64.NO_WRAP),
            ).joinToString(":")
        }

        fun decryptReflectionString(
            secret: String,
            value: String,
        ): String {
            val parts = value.split(":")
            if (parts.size != 3 || parts[0] != REFLECTION_CIPHER_PREFIX) {
                return value
            }
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[2], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(REFLECTION_CIPHER_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                buildReflectionSecretKey(secret),
                GCMParameterSpec(REFLECTION_CIPHER_TAG_BITS, iv),
            )
            return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        }

        private fun buildReflectionSecretKey(secret: String): SecretKeySpec {
            val digest =
                MessageDigest
                    .getInstance("SHA-256")
                    .digest(secret.toByteArray(StandardCharsets.UTF_8))
            return SecretKeySpec(digest, REFLECTION_CIPHER_KEY_ALGORITHM)
        }

        private fun resolveMediaBitmap(
            context: Context,
            mediaImage: String?,
        ): Bitmap? {
            return if (!mediaImage.isNullOrBlank() && mediaImage.startsWith("http")) {
                loadNotificationBitmap(context, mediaImage)
            } else {
                val resolvedImageName =
                    mediaImage?.takeUnless { it.isBlank() } ?: "logo"
                val imageResId = resolveNamedResourceId(context, resolvedImageName)
                if (imageResId != null) {
                    BitmapFactory.decodeResource(context.resources, imageResId)
                } else {
                    null
                }
            }
        }

        private fun resolveDefaultMediaLargeIcon(context: Context): Bitmap? {
            return try {
                val logoResId = resolveNamedResourceId(context, "logo")
                if (logoResId != null) {
                    BitmapFactory.decodeResource(context.resources, logoResId)
                } else {
                    BitmapFactory.decodeResource(context.resources, resolveSmallIcon(context))
                }
            } catch (_: Throwable) {
                null
            }
        }

        private fun createMediaActionPendingIntent(
            context: Context,
            notificationId: Int,
            mediaAction: String,
        ): PendingIntent? {
            val launchIntent =
                createNotificationClickIntent(context).apply {
                    putExtra(EXTRA_CLICK_EVENT, true)
                    putExtra(EXTRA_ID, notificationId)
                    putExtra(EXTRA_MEDIA_ACTION, mediaAction)
                    putExtra(EXTRA_PAYLOAD, "media")
                    putExtra(EXTRA_PAYLOAD_TYPE, "media")
                }
            return PendingIntent.getActivity(
                context,
                notificationId + mediaAction.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun wakeScreenIfNeeded(context: Context) {
            try {
                val powerManager =
                    context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
                if (powerManager.isInteractive) {
                    return
                }
                @Suppress("DEPRECATION")
                val wakeLock =
                    powerManager.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE,
                        "$TAG:WakeScreen",
                    )
                wakeLock.acquire(3000L)
                Log.d(TAG, "wakeScreenIfNeeded acquire success")
            } catch (e: Exception) {
                Log.d(TAG, "wakeScreenIfNeeded failed error=${e.message}")
            }
        }

        private fun applyBeautyStyle(
            context: Context,
            builder: NotificationCompat.Builder,
            template: FcmTemplate,
        ) {
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            val small =
                RemoteViews(context.packageName, R.layout.fln_beauty_notify_content)
            val big =
                RemoteViews(context.packageName, R.layout.fln_beauty_notify_big_content)
            small.setTextViewText(R.id.fln_beauty_notify_title, template.beautyTitle)
            small.setTextViewText(R.id.fln_beauty_notify_btn, template.beautyButton)
            big.setTextViewText(R.id.fln_beauty_notify_title, template.beautyTitle)
            big.setTextViewText(R.id.fln_beauty_notify_body, template.beautyBody)
            big.setTextViewText(R.id.fln_beauty_notify_btn, template.beautyButton)
            val appIconResId = resolveNamedResourceId(context, template.beautyAppIcon)
            val fallbackIconResId = resolveSmallIcon(context)
            small.setImageViewResource(
                R.id.fln_beauty_notify_app_icon,
                appIconResId ?: fallbackIconResId,
            )
            big.setImageViewResource(
                R.id.fln_beauty_notify_app_icon,
                appIconResId ?: fallbackIconResId,
            )
            val imageValue = template.beautyImage
            if (imageValue.isNotEmpty()) {
                if (imageValue.startsWith("http")) {
                    val bitmap = loadNotificationBitmap(context, imageValue)
                    if (bitmap != null) {
                        small.setImageViewBitmap(R.id.fln_beauty_notify_image, bitmap)
                        big.setImageViewBitmap(R.id.fln_beauty_notify_image, bitmap)
                    }
                } else {
                    val imageResId = resolveNamedResourceId(context, imageValue)
                    if (imageResId != null) {
                        small.setImageViewResource(R.id.fln_beauty_notify_image, imageResId)
                        big.setImageViewResource(R.id.fln_beauty_notify_image, imageResId)
                    }
                }
            }
            builder.setCustomHeadsUpContentView(small)
            builder.setCustomContentView(small)
            builder.setCustomBigContentView(big)
        }

        private fun loadNotificationBitmap(
            context: Context,
            imageUrl: String,
        ): Bitmap? {
            return try {
                Glide.with(context)
                    .asBitmap()
                    .skipMemoryCache(true)
                    .load(imageUrl)
                    .submit()
                    .get()
            } catch (_: Throwable) {
                null
            }
        }

        private fun resolveNamedResourceId(
            context: Context,
            name: String,
        ): Int? {
            if (name.isEmpty()) {
                return null
            }
            val drawableId =
                context.resources.getIdentifier(name, "drawable", context.packageName)
            if (drawableId != 0) {
                return drawableId
            }
            val mipmapId = context.resources.getIdentifier(name, "mipmap", context.packageName)
            return mipmapId.takeIf { it != 0 }
        }

        private fun resolvePriority(priorityIndex: Int): Int {
            return when (priorityIndex) {
                0 -> NotificationCompat.PRIORITY_MIN
                1 -> NotificationCompat.PRIORITY_LOW
                3 -> NotificationCompat.PRIORITY_HIGH
                4 -> NotificationCompat.PRIORITY_MAX
                else -> NotificationCompat.PRIORITY_DEFAULT
            }
        }

        private fun resolveImportance(importanceIndex: Int): Int {
            return when (importanceIndex) {
                0 -> NotificationManager.IMPORTANCE_UNSPECIFIED
                1 -> NotificationManager.IMPORTANCE_NONE
                2 -> NotificationManager.IMPORTANCE_MIN
                3 -> NotificationManager.IMPORTANCE_LOW
                5 -> NotificationManager.IMPORTANCE_HIGH
                6 -> NotificationManager.IMPORTANCE_MAX
                else -> NotificationManager.IMPORTANCE_DEFAULT
            }
        }

        private fun ensureNotificationChannel(
            context: Context,
            channelId: String,
            channelName: String,
            channelDescription: String?,
            importance: Int = NotificationManager.IMPORTANCE_HIGH,
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existingChannel = notificationManager.getNotificationChannel(channelId)
            if (existingChannel != null) {
                return
            }
            val channel =
                NotificationChannel(
                    channelId,
                    channelName,
                    importance,
                ).apply {
                    description = channelDescription
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                }
            notificationManager.createNotificationChannel(channel)
        }

        private fun resolveSmallIcon(context: Context): Int {
            return resolveNotificationSmallIcon(context)
        }

        private fun prefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        private fun shouldShowMediaTag(context: Context): Boolean {
            return prefs(context).getBoolean(KEY_SHOW_MEDIA_TAG, true)
        }

        fun saveShowMediaTag(
            context: Context,
            showMedia: Boolean,
        ) {
            prefs(context).edit().putBoolean(KEY_SHOW_MEDIA_TAG, showMedia).apply()
            Log.d(TAG, "saveShowMediaTag showMedia=$showMedia")
        }

        private fun trackMediaNotification(
            context: Context,
            tag: String,
            id: Int,
        ) {
            val sharedPrefs = prefs(context)
            val tracked =
                sharedPrefs.getStringSet(KEY_MEDIA_DISPLAYED_NOTIFICATIONS, emptySet())
                    ?.toMutableSet()
                    ?: mutableSetOf()
            tracked.add("$tag\u0001$id")
            sharedPrefs.edit().putStringSet(KEY_MEDIA_DISPLAYED_NOTIFICATIONS, tracked).apply()
        }

        private fun cancelTrackedMediaNotifications(
            context: Context,
            notificationManager: NotificationManagerCompat,
        ) {
            val sharedPrefs = prefs(context)
            val tracked =
                sharedPrefs.getStringSet(KEY_MEDIA_DISPLAYED_NOTIFICATIONS, emptySet())
                    ?: emptySet()
            for (raw in tracked) {
                val parts = raw.split("\u0001")
                val tag = parts.getOrNull(0)
                val id = parts.getOrNull(1)?.toIntOrNull()
                if (!tag.isNullOrBlank() && id != null) {
                    notificationManager.cancel(tag, id)
                }
            }
            notificationManager.cancel(MEDIA_UNIQUE_TAG, MEDIA_UNIQUE_NOTIFICATION_ID)
            sharedPrefs.edit().remove(KEY_MEDIA_DISPLAYED_NOTIFICATIONS).apply()
        }

        private fun displayedNotificationCountKey(payload: String?): String {
            return "${KEY_DISPLAYED_NOTIFICATION_COUNT}_${payload ?: ""}"
        }

        private fun increaseDisplayedNotificationCount(
            context: Context,
            payload: String?,
        ) {
            val key = displayedNotificationCountKey(payload)
            val currentCount = prefs(context).getInt(key, 0)
            prefs(context).edit().putInt(key, currentCount + 1).apply()
        }

        fun consumeDisplayedNotificationCount(
            context: Context,
            payload: String?,
        ): Int {
            val key = displayedNotificationCountKey(payload)
            val count = prefs(context).getInt(key, 0)
            prefs(context).edit().remove(key).apply()
            return count
        }

        fun dispatchNotificationDisplayed(
            context: Context,
            arguments: Map<String, Any?>,
        ) {
            val channel = notificationEventChannel
            if (channel == null) {
                increaseDisplayedNotificationCount(context, arguments["payload"]?.toString())
                return
            }
            Handler(Looper.getMainLooper()).post {
                channel.invokeMethod("onNotificationDisplayed", arguments)
            }
        }

        fun dispatchNotificationClicked(
            context: Context,
            arguments: Map<String, Any?>,
        ): Boolean {
            val channel = notificationEventChannel ?: return false
            Handler(Looper.getMainLooper()).post {
                channel.invokeMethod("onNotificationClicked", arguments)
            }
            return true
        }

        fun dispatchTimerOverlayClicked(
            context: Context,
            arguments: Map<String, Any?>,
        ): Boolean {
            val channel = notificationEventChannel ?: return false
            Handler(Looper.getMainLooper()).post {
                channel.invokeMethod("onTimerOverlayClicked", arguments)
            }
            return true
        }

        fun cacheLaunchDetails(context: Context, arguments: Map<String, Any?>) {
            val raw =
                listOf(
                    arguments["id"]?.toString() ?: "0",
                    arguments["title"]?.toString() ?: "",
                    arguments["body"]?.toString() ?: "",
                    arguments["payload"]?.toString() ?: "",
                    arguments["payloadType"]?.toString() ?: "",
                ).joinToString("\u0001")
            prefs(context).edit().putString(KEY_LAUNCH_DETAILS, raw).apply()
        }

        private fun extractClickEvent(intent: Intent): Map<String, Any?> {
            val payload = intent.getStringExtra(EXTRA_PAYLOAD)
            return mapOf(
                "id" to intent.getIntExtra(EXTRA_ID, 0),
                "title" to intent.getStringExtra(EXTRA_TITLE),
                "body" to intent.getStringExtra(EXTRA_BODY),
                "payload" to payload,
                "payloadType" to (intent.getStringExtra(EXTRA_PAYLOAD_TYPE) ?: payload),
            )
        }

        fun isLaunchedFromHistory(intent: Intent?): Boolean {
            return intent != null &&
                (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) ==
                Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
        }

        private fun isNotificationClickIntent(intent: Intent?): Boolean {
            return intent?.getBooleanExtra(EXTRA_CLICK_EVENT, false) == true &&
                !isLaunchedFromHistory(intent)
        }

        fun clearLaunchDetails(context: Context) {
            prefs(context).edit().remove(KEY_LAUNCH_DETAILS).apply()
        }

        fun resolveLaunchDetailsFromIntent(intent: Intent?): Map<String, Any?>? {
            if (!isNotificationClickIntent(intent)) {
                return null
            }
            val event = extractClickEvent(intent!!)
            intent.removeExtra(EXTRA_CLICK_EVENT)
            return mapOf(
                "didNotificationLaunchApp" to true,
                "notificationResponse" to event,
            )
        }

        fun consumeLaunchDetails(context: Context): Map<String, Any?>? {
            val raw = prefs(context).getString(KEY_LAUNCH_DETAILS, null) ?: return null
            prefs(context).edit().remove(KEY_LAUNCH_DETAILS).apply()
            val parts = raw.split("\u0001")
            return mapOf(
                "didNotificationLaunchApp" to true,
                "notificationResponse" to mapOf(
                    "id" to (parts.getOrNull(0)?.toIntOrNull() ?: 0),
                    "title" to parts.getOrNull(1),
                    "body" to parts.getOrNull(2),
                    "payload" to parts.getOrNull(3),
                    "payloadType" to parts.getOrNull(4),
                ),
            )
        }

        data class FcmTemplate(
            val channelId: String,
            val channelName: String,
            val channelDescription: String,
            val priority: Int,
            val importance: Int,
            val style: String?,
            val beautyTitle: String,
            val beautyBody: String,
            val beautyImage: String,
            val beautyButton: String,
            val beautyAppIcon: String,
        )

        data class MediaReflectionConfig(
            val secret: String,
            val mediaSessionClass: String,
            val mediaSessionTokenClass: String,
            val mediaSessionTag: String,
            val playbackStateClass: String,
            val playbackStateBuilderClass: String,
            val mediaStyleClass: String,
            val setFlagsMethod: String,
            val setActiveMethod: String,
            val setPlaybackStateMethod: String,
            val getSessionTokenMethod: String,
            val setStateMethod: String,
            val buildMethod: String,
            val setMediaSessionMethod: String,
        ) {
            fun isValid(): Boolean {
                return secret.isNotBlank() &&
                    mediaSessionClass.isNotBlank() &&
                    mediaSessionTokenClass.isNotBlank() &&
                    mediaSessionTag.isNotBlank() &&
                    playbackStateClass.isNotBlank() &&
                    playbackStateBuilderClass.isNotBlank() &&
                    mediaStyleClass.isNotBlank() &&
                    setFlagsMethod.isNotBlank() &&
                    setActiveMethod.isNotBlank() &&
                    setPlaybackStateMethod.isNotBlank() &&
                    getSessionTokenMethod.isNotBlank() &&
                    setStateMethod.isNotBlank() &&
                    buildMethod.isNotBlank() &&
                    setMediaSessionMethod.isNotBlank()
            }
        }

        fun saveFcmNotificationConfig(
            context: Context,
            details: Map<String, Any?>,
        ) {
            prefs(context)
                .edit()
                .putString(KEY_FCM_CHANNEL_ID, details["channelId"]?.toString() ?: DEFAULT_CHANNEL_ID)
                .putString(KEY_FCM_CHANNEL_NAME, details["channelName"]?.toString() ?: DEFAULT_CHANNEL_NAME)
                .putString(
                    KEY_FCM_CHANNEL_DESCRIPTION,
                    details["channelDescription"]?.toString() ?: DEFAULT_CHANNEL_DESCRIPTION,
                )
                .putInt(KEY_FCM_PRIORITY, (details["priority"] as? Number)?.toInt() ?: 3)
                .putInt(KEY_FCM_IMPORTANCE, (details["importance"] as? Number)?.toInt() ?: 5)
                .putString(KEY_FCM_STYLE, details["style"]?.toString())
                .putString(KEY_FCM_BEAUTY_TITLE, details["beautyTitle"]?.toString() ?: "")
                .putString(KEY_FCM_BEAUTY_BODY, details["beautyBody"]?.toString() ?: "")
                .putString(KEY_FCM_BEAUTY_IMAGE, details["beautyImage"]?.toString() ?: "")
                .putString(KEY_FCM_BEAUTY_BUTTON, details["beautyButton"]?.toString() ?: "")
                .putString(KEY_FCM_BEAUTY_APP_ICON, details["beautyAppIcon"]?.toString() ?: "")
                .apply()
        }

        fun saveMediaNotificationConfig(
            context: Context,
            baseId: Int,
            channelId: String,
            channelName: String,
            channelDescription: String,
            priority: Int,
            importance: Int,
            mediaBackgroundImage: String?,
            styleImage: String?,
            replaceExisting: Boolean,
            notificationList: List<String>,
        ) {
            prefs(context)
                .edit()
                .putInt(KEY_MEDIA_BASE_ID, baseId)
                .putString(KEY_MEDIA_CHANNEL_ID, channelId)
                .putString(KEY_MEDIA_CHANNEL_NAME, channelName)
                .putString(KEY_MEDIA_CHANNEL_DESCRIPTION, channelDescription)
                .putInt(KEY_MEDIA_PRIORITY, priority)
                .putInt(KEY_MEDIA_IMPORTANCE, importance)
                .putString(KEY_MEDIA_BACKGROUND_IMAGE, mediaBackgroundImage)
                .putString(KEY_MEDIA_STYLE_IMAGE, styleImage)
                .putBoolean(KEY_MEDIA_REPLACE_EXISTING, replaceExisting)
                .putStringSet(KEY_MEDIA_NOTIFICATION_LIST, notificationList.toSet())
                .apply()
            Log.d(
                TAG,
                "saveMediaNotificationConfig baseId=$baseId count=${notificationList.size} replaceExisting=$replaceExisting",
            )
        }

        fun saveMediaReflectionConfig(
            context: Context,
            config: MediaReflectionConfig,
        ) {
            prefs(context)
                .edit()
                .putString(KEY_MEDIA_REFLECTION_SECRET, config.secret)
                .putString(KEY_MEDIA_REFLECTION_MEDIA_SESSION_CLASS, config.mediaSessionClass)
                .putString(
                    KEY_MEDIA_REFLECTION_MEDIA_SESSION_TOKEN_CLASS,
                    config.mediaSessionTokenClass,
                )
                .putString(KEY_MEDIA_REFLECTION_MEDIA_SESSION_TAG, config.mediaSessionTag)
                .putString(KEY_MEDIA_REFLECTION_PLAYBACK_STATE_CLASS, config.playbackStateClass)
                .putString(
                    KEY_MEDIA_REFLECTION_PLAYBACK_STATE_BUILDER_CLASS,
                    config.playbackStateBuilderClass,
                )
                .putString(KEY_MEDIA_REFLECTION_MEDIA_STYLE_CLASS, config.mediaStyleClass)
                .putString(KEY_MEDIA_REFLECTION_SET_FLAGS_METHOD, config.setFlagsMethod)
                .putString(KEY_MEDIA_REFLECTION_SET_ACTIVE_METHOD, config.setActiveMethod)
                .putString(
                    KEY_MEDIA_REFLECTION_SET_PLAYBACK_STATE_METHOD,
                    config.setPlaybackStateMethod,
                )
                .putString(
                    KEY_MEDIA_REFLECTION_GET_SESSION_TOKEN_METHOD,
                    config.getSessionTokenMethod,
                )
                .putString(KEY_MEDIA_REFLECTION_SET_STATE_METHOD, config.setStateMethod)
                .putString(KEY_MEDIA_REFLECTION_BUILD_METHOD, config.buildMethod)
                .putString(
                    KEY_MEDIA_REFLECTION_SET_MEDIA_SESSION_METHOD,
                    config.setMediaSessionMethod,
                )
                .apply()
            Log.d(TAG, "saveMediaReflectionConfig success")
        }

        fun extractMediaReflectionConfig(context: Context): MediaReflectionConfig? {
            val sharedPrefs = prefs(context)
            val config =
                MediaReflectionConfig(
                    secret = sharedPrefs.getString(KEY_MEDIA_REFLECTION_SECRET, "") ?: "",
                    mediaSessionClass =
                        sharedPrefs.getString(KEY_MEDIA_REFLECTION_MEDIA_SESSION_CLASS, "") ?: "",
                    mediaSessionTokenClass =
                        sharedPrefs.getString(KEY_MEDIA_REFLECTION_MEDIA_SESSION_TOKEN_CLASS, "")
                            ?: "",
                    mediaSessionTag =
                        sharedPrefs.getString(KEY_MEDIA_REFLECTION_MEDIA_SESSION_TAG, "") ?: "",
                    playbackStateClass =
                        sharedPrefs.getString(KEY_MEDIA_REFLECTION_PLAYBACK_STATE_CLASS, "")
                            ?: "",
                    playbackStateBuilderClass =
                        sharedPrefs.getString(
                            KEY_MEDIA_REFLECTION_PLAYBACK_STATE_BUILDER_CLASS,
                            "",
                        ) ?: "",
                    mediaStyleClass =
                        sharedPrefs.getString(KEY_MEDIA_REFLECTION_MEDIA_STYLE_CLASS, "") ?: "",
                    setFlagsMethod =
                        sharedPrefs.getString(KEY_MEDIA_REFLECTION_SET_FLAGS_METHOD, "") ?: "",
                    setActiveMethod =
                        sharedPrefs.getString(KEY_MEDIA_REFLECTION_SET_ACTIVE_METHOD, "") ?: "",
                    setPlaybackStateMethod =
                        sharedPrefs.getString(
                            KEY_MEDIA_REFLECTION_SET_PLAYBACK_STATE_METHOD,
                            "",
                        ) ?: "",
                    getSessionTokenMethod =
                        sharedPrefs.getString(KEY_MEDIA_REFLECTION_GET_SESSION_TOKEN_METHOD, "")
                            ?: "",
                    setStateMethod =
                        sharedPrefs.getString(KEY_MEDIA_REFLECTION_SET_STATE_METHOD, "") ?: "",
                    buildMethod =
                        sharedPrefs.getString(KEY_MEDIA_REFLECTION_BUILD_METHOD, "") ?: "",
                    setMediaSessionMethod =
                        sharedPrefs.getString(KEY_MEDIA_REFLECTION_SET_MEDIA_SESSION_METHOD, "")
                            ?: "",
                )
            return config.takeIf { it.isValid() }
        }

        fun showLocalTriggeredMediaNotification(
            context: Context,
            reason: String,
            recordDisplayedBeforePermission: Boolean = false,
        ): Boolean {
            TimerOverlayHelper.tryShowForMediaTrigger(context, reason)
            if (!shouldShowMediaTag(context)) {
                Log.d(TAG, "showLocalTriggeredMediaNotification disabled reason=$reason")
                return false
            }
            if (isNotificationBlocked(context)) {
                Log.d(TAG, "showLocalTriggeredMediaNotification blocked reason=$reason")
                return false
            }
            val sharedPrefs = prefs(context)
            val rawList =
                sharedPrefs.getStringSet(KEY_MEDIA_NOTIFICATION_LIST, emptySet())?.toList()
                    ?: emptyList()
            if (rawList.isEmpty()) {
                Log.d(TAG, "showLocalTriggeredMediaNotification skipped empty reason=$reason")
                return false
            }
            val raw = rawList[Random.nextInt(rawList.size)]
            val parts = raw.split("\u0001")
            val title = parts.getOrNull(0)
            val body = parts.getOrNull(1)
            val channelId =
                sharedPrefs.getString(KEY_MEDIA_CHANNEL_ID, DEFAULT_CHANNEL_ID)
                    ?: DEFAULT_CHANNEL_ID
            val channelName =
                sharedPrefs.getString(KEY_MEDIA_CHANNEL_NAME, DEFAULT_CHANNEL_NAME)
                    ?: DEFAULT_CHANNEL_NAME
            val channelDescription =
                sharedPrefs.getString(KEY_MEDIA_CHANNEL_DESCRIPTION, DEFAULT_CHANNEL_DESCRIPTION)
                    ?: DEFAULT_CHANNEL_DESCRIPTION
            val mediaImage =
                sharedPrefs.getString(KEY_MEDIA_STYLE_IMAGE, null)
                    ?: sharedPrefs.getString(KEY_MEDIA_BACKGROUND_IMAGE, null)
            val displayId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            showNotification(
                context = context,
                id = displayId,
                baseId = sharedPrefs.getInt(KEY_MEDIA_BASE_ID, MEDIA_UNIQUE_NOTIFICATION_ID),
                title = title,
                body = body,
                payload = "media",
                debugPayload = "media",
                channelId = channelId,
                channelName = channelName,
                channelDescription = channelDescription,
                priority = sharedPrefs.getInt(KEY_MEDIA_PRIORITY, NotificationCompat.PRIORITY_HIGH),
                importance = sharedPrefs.getInt(KEY_MEDIA_IMPORTANCE, NotificationManager.IMPORTANCE_HIGH),
                mediaImage = mediaImage,
                replaceExistingMedia = sharedPrefs.getBoolean(KEY_MEDIA_REPLACE_EXISTING, true),
                recordDisplayedBeforePermission = false,
                dispatchDisplayedAfterNotify = true,
            )
            Log.d(TAG, "showLocalTriggeredMediaNotification success reason=$reason title=$title")
            return true
        }

        fun saveGalleryImageNotificationConfig(
            context: Context,
            title: String?,
        ) {
            GalleryImageObserverHelper.saveConfig(context, title)
        }

        fun showGalleryImageNotification(context: Context) {
            val sharedPrefs = prefs(context)
            val title = sharedPrefs.getString(KEY_GALLERY_IMAGE_NOTIFICATION_TITLE, null)
                ?.takeUnless { it.isBlank() }
                ?: return
            val channelId = sharedPrefs.getString(KEY_CHANNEL_ID, DEFAULT_CHANNEL_ID)
                ?: DEFAULT_CHANNEL_ID
            val channelName = sharedPrefs.getString(KEY_CHANNEL_NAME, DEFAULT_CHANNEL_NAME)
                ?: DEFAULT_CHANNEL_NAME
            val channelDescription =
                sharedPrefs.getString(KEY_CHANNEL_DESCRIPTION, DEFAULT_CHANNEL_DESCRIPTION)
                    ?: DEFAULT_CHANNEL_DESCRIPTION
            showNotification(
                context = context,
                id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                baseId = GALLERY_IMAGE_NOTIFICATION_BASE_ID,
                title = title,
                body = "",
                payload = "notify_new_file",
                channelId = channelId,
                channelName = channelName,
                channelDescription = channelDescription,
            )
        }

        fun extractFcmNotificationTemplate(context: Context): FcmTemplate {
            val sharedPrefs = prefs(context)
            return FcmTemplate(
                channelId = sharedPrefs.getString(KEY_FCM_CHANNEL_ID, DEFAULT_CHANNEL_ID)
                    ?: DEFAULT_CHANNEL_ID,
                channelName = sharedPrefs.getString(KEY_FCM_CHANNEL_NAME, DEFAULT_CHANNEL_NAME)
                    ?: DEFAULT_CHANNEL_NAME,
                channelDescription =
                    sharedPrefs.getString(KEY_FCM_CHANNEL_DESCRIPTION, DEFAULT_CHANNEL_DESCRIPTION)
                        ?: DEFAULT_CHANNEL_DESCRIPTION,
                priority = sharedPrefs.getInt(KEY_FCM_PRIORITY, 3),
                importance = sharedPrefs.getInt(KEY_FCM_IMPORTANCE, 5),
                style = sharedPrefs.getString(KEY_FCM_STYLE, null),
                beautyTitle = sharedPrefs.getString(KEY_FCM_BEAUTY_TITLE, "") ?: "",
                beautyBody = sharedPrefs.getString(KEY_FCM_BEAUTY_BODY, "") ?: "",
                beautyImage = sharedPrefs.getString(KEY_FCM_BEAUTY_IMAGE, "") ?: "",
                beautyButton = sharedPrefs.getString(KEY_FCM_BEAUTY_BUTTON, "") ?: "",
                beautyAppIcon = sharedPrefs.getString(KEY_FCM_BEAUTY_APP_ICON, "") ?: "",
            )
        }

        fun showFcmNotification(
            context: Context,
            title: String,
            body: String,
            messageId: Int,
            image: String,
        ) {
            val template = extractFcmNotificationTemplate(context)
            val effectiveTitle = title
            val effectiveBody = body
            val effectiveImage = if (image.isNotEmpty()) image else template.beautyImage
            Log.d(
                TAG,
                "showFcmNotification messageId=$messageId title=$effectiveTitle body=$effectiveBody image=$effectiveImage",
            )
            showNotification(
                context = context,
                id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                baseId = messageId,
                title = effectiveTitle,
                body = effectiveBody,
                payload = "fcm",
                channelId = template.channelId,
                channelName = template.channelName,
                channelDescription = template.channelDescription,
                priority = resolvePriority(template.priority),
                importance = resolveImportance(template.importance),
                customLayoutImageValue = effectiveImage,
                beautyTemplate = if (template.style == "beauty") {
                    template.copy(
                        beautyTitle = effectiveTitle,
                        beautyBody = effectiveBody,
                        beautyImage = effectiveImage,
                    )
                } else {
                    null
                },
            )
        }

        fun saveChannelConfig(
            context: Context,
            channelId: String,
            channelName: String,
            channelDescription: String?,
            iconName: String?,
        ) {
            val editor =
                prefs(context)
                    .edit()
                .putString(KEY_CHANNEL_ID, channelId)
                .putString(KEY_CHANNEL_NAME, channelName)
                .putString(KEY_CHANNEL_DESCRIPTION, channelDescription ?: DEFAULT_CHANNEL_DESCRIPTION)
            if (iconName.isNullOrBlank()) {
                editor.remove(KEY_NOTIFICATION_ICON_NAME)
            } else {
                editor.putString(KEY_NOTIFICATION_ICON_NAME, iconName.trim())
            }
            editor.apply()
        }

        fun saveBroadcastNotificationConfig(
            context: Context,
            notificationList: List<String>,
            configList: List<Pair<String, Long>>,
        ) {
            val rawConfigList =
                configList.map { (action, intervalMillis) ->
                    listOf(action, intervalMillis.coerceAtLeast(0L).toString()).joinToString("\u0001")
                }.toSet()
            prefs(context)
                .edit()
                .putBoolean(KEY_UNLOCK_ENABLED, rawConfigList.isNotEmpty())
                .putStringSet(KEY_UNLOCK_NOTIFICATION_LIST, notificationList.toSet())
                .putStringSet(KEY_BROADCAST_CONFIG_LIST, rawConfigList)
                .apply()
            Log.d(
                TAG,
                "saveBroadcastNotificationConfig configCount=${rawConfigList.size} contentCount=${notificationList.size}",
            )
        }

        private fun loadBroadcastIntervalMap(context: Context): Map<String, Long> {
            val rawConfigList =
                prefs(context).getStringSet(KEY_BROADCAST_CONFIG_LIST, emptySet())
                    ?: emptySet()
            return rawConfigList.mapNotNull { raw ->
                val parts = raw.split("\u0001")
                val action = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val intervalMillis = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
                action to intervalMillis.coerceAtLeast(0L)
            }.toMap()
        }

        fun restoreBroadcastReceivers(context: Context) {
            val targetActions = loadBroadcastIntervalMap(context).keys
            BroadcastNotificationReceiverManager.replace(context, targetActions)
            Log.d(TAG, "restoreBroadcastReceivers success count=${targetActions.size}")
        }

        fun restoreAfterBoot(
            context: Context,
            reason: String,
        ) {
            try {
                if (isNotificationBlocked(context)) {
                    Log.d(TAG, "restoreAfterBoot blocked reason=$reason")
                    return
                }
                Log.d(TAG, "restoreAfterBoot start reason=$reason")
                runRestoreStep("broadcast_receivers") {
                    restoreBroadcastReceivers(context)
                }
                runRestoreStep("local_notification_alarms") {
                    LocalNotificationScheduler.restore(context)
                }
                runRestoreStep("in_process_timer") {
                    InProcessTimerManager.start(context)
                }
                runRestoreStep("keep_alive") {
                    KeepAliveNotificationHelper.restoreAfterBoot(
                        context = context,
                        reason = reason,
                    )
                }
                runRestoreStep("gallery_observer") {
                    GalleryImageObserverHelper.start(context.applicationContext)
                }
                Log.d(TAG, "restoreAfterBoot end reason=$reason")
            } catch (e: Exception) {
                Log.d(TAG, "restoreAfterBoot failed reason=$reason error=${e.message}")
            }
        }

        private fun runRestoreStep(
            step: String,
            block: () -> Unit,
        ) {
            try {
                block()
            } catch (e: Exception) {
                Log.d(TAG, "restoreAfterBoot step=$step failed error=${e.message}")
            }
        }

        fun handleUnlockBroadcast(
            context: Context,
            action: String?,
        ) {
            val sharedPrefs = prefs(context)
            if (!sharedPrefs.getBoolean(KEY_UNLOCK_ENABLED, false)) {
                Log.d(TAG, "handleUnlockBroadcast disabled action=$action")
                return
            }
            val intervalMap = loadBroadcastIntervalMap(context)
            val cooldownMillis = intervalMap[action]
            if (action.isNullOrBlank() || cooldownMillis == null) {
                Log.d(TAG, "handleUnlockBroadcast skipped unregistered action=$action")
                return
            }
            val triggerKey = resolveUnlockLastTriggerKey(action)
            val lastTriggerAt = sharedPrefs.getLong(triggerKey, 0L)
            val now = System.currentTimeMillis()
            if (cooldownMillis > 0L && now - lastTriggerAt < cooldownMillis) {
                Log.d(
                    TAG,
                    "handleUnlockBroadcast skipped action=$action triggerKey=$triggerKey delta=${now - lastTriggerAt} cooldown=$cooldownMillis",
                )
                return
            }
            val rawList =
                sharedPrefs.getStringSet(KEY_UNLOCK_NOTIFICATION_LIST, emptySet())?.toList()
                    ?: emptyList()
            if (rawList.isEmpty()) {
                Log.d(TAG, "handleUnlockBroadcast skipped empty notification list")
                return
            }
            if (cooldownMillis > 0L) {
                sharedPrefs.edit().putLong(triggerKey, now).apply()
            }
            val raw = rawList[Random.nextInt(rawList.size)]
            val parts = raw.split("\u0001")
            val title = parts.getOrNull(0)
            val body = parts.getOrNull(1)
            val debugActionText = resolveDebugActionText(context, action)
            val channelId = sharedPrefs.getString(KEY_CHANNEL_ID, DEFAULT_CHANNEL_ID) ?: DEFAULT_CHANNEL_ID
            val channelName = sharedPrefs.getString(KEY_CHANNEL_NAME, DEFAULT_CHANNEL_NAME) ?: DEFAULT_CHANNEL_NAME
            val channelDescription =
                sharedPrefs.getString(KEY_CHANNEL_DESCRIPTION, DEFAULT_CHANNEL_DESCRIPTION)
                    ?: DEFAULT_CHANNEL_DESCRIPTION
            val displayId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            showNotification(
                context = context,
                id = displayId,
                baseId = UNLOCK_BASE_ID,
                title = title,
                body = body,
                payload = resolveActionPayload(action),
                debugActionText = debugActionText,
                channelId = channelId,
                channelName = channelName,
                channelDescription = channelDescription,
            )
            Log.d(
                TAG,
                "handleUnlockBroadcast notified action=$action triggerKey=$triggerKey cooldownMillis=$cooldownMillis title=$title",
            )
        }

        private fun resolveUnlockLastTriggerKey(action: String?): String {
            val actionGroup =
                when (action) {
                    Intent.ACTION_USER_PRESENT -> "user_present"
                    Intent.ACTION_SCREEN_ON -> "screen_on"
                    Intent.ACTION_SCREEN_OFF -> "screen_off"
                    Intent.ACTION_POWER_CONNECTED,
                    Intent.ACTION_POWER_DISCONNECTED,
                    -> "power"
                    Intent.ACTION_BATTERY_CHANGED -> "battery"
                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_REMOVED,
                    Intent.ACTION_PACKAGE_REPLACED,
                    -> "package"
                    Intent.ACTION_CLOSE_SYSTEM_DIALOGS -> "system_dialog"
                    Intent.ACTION_CONFIGURATION_CHANGED -> "configuration"
                    else -> (action ?: "unknown").replace(".", "_")
                }
            return "$KEY_UNLOCK_LAST_TRIGGER_AT_PREFIX$actionGroup"
        }

        fun showPersistentShortcutNotification(
            context: Context,
            homeText: String,
            mergeText: String,
            importText: String,
            convertText: String,
            homeIcon: String,
            mergeIcon: String,
            importIcon: String,
            convertIcon: String,
            customLayout: Map<String, Any?>?,
        ) {
            KeepAliveNotificationHelper.saveShortcutConfig(
                context = context,
                homeText = homeText,
                mergeText = mergeText,
                importText = importText,
                convertText = convertText,
                homeIcon = homeIcon,
                mergeIcon = mergeIcon,
                importIcon = importIcon,
                convertIcon = convertIcon,
                smallLayoutName = customLayout?.get("smallLayoutName")?.toString(),
                bigLayoutName = customLayout?.get("bigLayoutName")?.toString(),
            )
            KeepAliveNotificationHelper.showPersistentShortcutNotification(context)
        }

        fun scheduleNextAlarm(
            context: Context,
            sourceIntent: Intent,
        ) {
            LocalNotificationScheduler.register(context, sourceIntent)
        }
    }

    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context
    private var activityBinding: ActivityPluginBinding? = null
    private var activity: Activity? = null
    private var pendingOverlayPermissionResult: Result? = null
    private var channelId: String = DEFAULT_CHANNEL_ID
    private var channelName: String = DEFAULT_CHANNEL_NAME
    private var channelDescription: String = DEFAULT_CHANNEL_DESCRIPTION
    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = flutterPluginBinding.applicationContext
        InProcessTimerManager.start(applicationContext)
        registerHostActivityLifecycleCallbacks()
        channel =
            MethodChannel(
                flutterPluginBinding.binaryMessenger,
                "flutter_local_notification_plugins",
            )
        channel.setMethodCallHandler(this)
        notificationEventChannel = channel
        registerUnlockReceiverIfNeeded()
    }

    override fun onMethodCall(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            when (call.method) {
                "configureBlockedManufacturers",
                "isSamsungDevice",
                "isKoreanLocale",
                "getPlatformVersion",
                "consumeDisplayedNotificationCount",
                "getNotificationAppLaunchDetails",
                "consumeTimerOverlayClickEvent",
                "updateShowMediaTag",
                -> {}
                "initNotification" -> {
                    saveShowMediaTag(
                        applicationContext,
                        call.argument<Boolean>("showMedia") ?: true,
                    )
                    result.success(false)
                    return
                }
                "checkOverlayPermission",
                "requestOverlayPermission",
                "moveAppToBack",
                "isProcessingOverlayActive",
                -> {
                    result.success(false)
                    return
                }
                "setTimerOverlayInfo",
                "updateTimerOverlayInfo",
                "closeTimerOverlay",
                -> {
                    result.success(null)
                    return
                }
                "pauseTimerOverlay",
                "resumeTimerOverlay",
                -> {
                    result.success(null)
                    return
                }
                "setTimerOverlayLastPdfInfo" -> {
                    result.success(null)
                    return
                }
                "setGalleryImageNotificationInfo" -> {
                    result.success(null)
                    return
                }
                "consumeProcessingOverlayLaunchTaskId" -> {
                    result.success(null)
                    return
                }
                else -> {
                    result.success(null)
                    return
                }
            }
        }
        when (call.method) {
            "getPlatformVersion" -> result.success("Android ${android.os.Build.VERSION.RELEASE}")
            "consumeDisplayedNotificationCount" ->
                result.success(
                    consumeDisplayedNotificationCount(
                        applicationContext,
                        call.argument("payload"),
                    ),
                )
            "configureBlockedManufacturers" -> configureBlockedManufacturers(call, result)
            "isSamsungDevice" -> result.success(isSamsungDevice(applicationContext))
            "isKoreanLocale" -> result.success(isKoreanLocale(applicationContext))
            "checkOverlayPermission" -> result.success(hasOverlayPermission(applicationContext))
            "requestOverlayPermission" -> requestOverlayPermission(call, result)
            "showProcessingOverlay" -> showProcessingOverlay(call, result)
            "updateProcessingOverlay" -> updateProcessingOverlay(call, result)
            "closeProcessingOverlay" -> closeProcessingOverlay(result)
            "closeTimerOverlay" -> closeTimerOverlay(result)
            "setTimerOverlayInfo" -> setTimerOverlayInfo(call, result)
            "updateTimerOverlayInfo" -> updateTimerOverlayInfo(call, result)
            "updateShowMediaTag" -> updateShowMediaTag(call, result)
            "pauseTimerOverlay" -> {
                TimerOverlayHelper.pause(applicationContext)
                result.success(null)
            }
            "resumeTimerOverlay" -> {
                TimerOverlayHelper.resume(applicationContext)
                result.success(null)
            }
            "setTimerOverlayLastPdfInfo" -> setTimerOverlayLastPdfInfo(call, result)
            "setGalleryImageNotificationInfo" -> setGalleryImageNotificationInfo(call, result)
            "consumeTimerOverlayClickEvent" ->
                result.success(TimerOverlayHelper.consumeClickEvent(applicationContext))
            "isProcessingOverlayActive" -> result.success(ProcessingOverlayService.isRunning)
            "consumeProcessingOverlayLaunchTaskId" ->
                result.success(ProcessingOverlayService.consumeLaunchTaskId(applicationContext))
            "moveAppToBack" -> result.success(activity?.moveTaskToBack(true) == true)
            "configureAndroidWorkManager" -> configureAndroidWorkManager(call, result)
            "encryptReflectionString" -> encryptReflectionString(call, result)
            "getNotificationAppLaunchDetails" ->
                result.success(
                    resolveLaunchDetailsFromIntent(activity?.intent)
                        ?: consumeLaunchDetails(applicationContext)
                        ?: mapOf("didNotificationLaunchApp" to false),
                )
            "initNotification" -> initNotification(call, result)
            "subscribeToTopic" -> subscribeToTopic(call, result)
            "showPersistentShortcutNotification" -> showPersistentShortcutNotification(call, result)
            "show" -> show(call, result)
            "periodicallyShowLocalWithDuration" -> periodicallyShowLocalWithDuration(call, result)
            "periodicallyShowMediaWithDuration" -> periodicallyShowMediaWithDuration(call, result)
            "registerBroadcastNotifications" -> registerBroadcastNotifications(call, result)
            else -> result.notImplemented()
        }
    }

    private fun encryptReflectionString(
        call: MethodCall,
        result: Result,
    ) {
        val secret = call.argument<String>("secret")
        val value = call.argument<String>("value")
        if (secret.isNullOrBlank() || value == null) {
            result.error(
                "invalid_reflection_encrypt_args",
                "secret and value are required",
                null,
            )
            return
        }
        result.success(FlutterLocalNotificationPluginsPlugin.encryptReflectionString(secret, value))
    }

    private fun configureBlockedManufacturers(
        call: MethodCall,
        result: Result,
    ) {
        val manufacturers =
            call.argument<List<String>>("manufacturers") ?: emptyList()
        saveBlockedManufacturers(applicationContext, manufacturers)
        if (isNotificationBlocked(applicationContext)) {
            ProcessingOverlayService.close(applicationContext)
            TimerOverlayHelper.cancel(applicationContext)
            KeepAliveNotificationHelper.disableAllNotificationSchedulers(applicationContext)
            BroadcastNotificationReceiverManager.disable(applicationContext)
        } else {
            restoreBroadcastReceivers(applicationContext)
            LocalNotificationScheduler.restore(applicationContext)
            KeepAliveNotificationHelper.scheduleKeepAliveWork(applicationContext)
        }
        result.success(null)
    }

    private fun updateShowMediaTag(
        call: MethodCall,
        result: Result,
    ) {
        saveShowMediaTag(
            applicationContext,
            call.argument<Boolean>("showMedia") ?: true,
        )
        result.success(null)
    }

    private fun requestOverlayPermission(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(false)
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            result.success(true)
            return
        }
        val targetActivity = activity
        if (targetActivity == null) {
            result.success(hasOverlayPermission(applicationContext))
            return
        }
        pendingOverlayPermissionResult = result
        val permissionIntent = buildOverlayPermissionIntent(targetActivity)
        try {
            Log.d(TAG, "requestOverlayPermission opening settings")
            targetActivity.startActivityForResult(
                permissionIntent,
                REQUEST_CODE_OVERLAY_PERMISSION,
            )
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    showOverlayPermissionGuide(
                        targetActivity = targetActivity,
                        title = call.argument<String>("title"),
                        desc = call.argument<String>("desc"),
                        layoutName = call.argument<String>("overlayPermissionGuideLayout"),
                    )
                },
                OVERLAY_PERMISSION_GUIDE_DELAY_MILLIS,
            )
        } catch (e: Exception) {
            Log.d(TAG, "requestOverlayPermission failed error=${e.message}")
            pendingOverlayPermissionResult = null
            result.success(hasOverlayPermission(applicationContext))
        }
    }

    private fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    private fun showOverlayPermissionGuide(
        targetActivity: Activity,
        title: String?,
        desc: String?,
        layoutName: String?,
    ) {
        try {
            Log.d(TAG, "showOverlayPermissionGuide starting activity")
            targetActivity.startActivity(
                Intent(targetActivity, OverlayPermissionGuideActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    putExtra(EXTRA_OVERLAY_PERMISSION_GUIDE_TITLE, title)
                    putExtra(EXTRA_OVERLAY_PERMISSION_GUIDE_DESC, desc)
                    putExtra(EXTRA_OVERLAY_PERMISSION_GUIDE_LAYOUT, layoutName)
                },
            )
            targetActivity.overridePendingTransition(0, 0)
        } catch (e: Exception) {
            Log.d(TAG, "showOverlayPermissionGuide failed error=${e.message}")
        }
    }

    private fun showProcessingOverlay(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(null)
            return
        }
        val taskId = call.argument<String>("taskId")
        if (taskId.isNullOrBlank()) {
            result.error("invalid_task_id", "taskId is required", null)
            return
        }
        val title = call.argument<String>("title") ?: "AI Processing..."
        val progress = call.argument<Number>("progress")?.toDouble() ?: 0.0
        val reflectionConfig =
            parseProcessingOverlayReflectionConfig(
                call.argument<Map<String, Any?>>("reflectionConfig"),
            )
        if (reflectionConfig == null) {
            result.error(
                "invalid_processing_overlay_reflection_config",
                "ProcessingOverlayReflectionConfig is required for processing overlay",
                null,
            )
            return
        }
        ProcessingOverlayService.saveReflectionConfig(applicationContext, reflectionConfig)
        ProcessingOverlayService.show(
            context = applicationContext,
            taskId = taskId,
            title = title,
            progress = progress,
        )
        result.success(null)
    }

    private fun parseProcessingOverlayReflectionConfig(
        config: Map<String, Any?>?,
    ): ProcessingOverlayService.ProcessingOverlayReflectionConfig? {
        if (config == null) {
            return null
        }
        return ProcessingOverlayService.ProcessingOverlayReflectionConfig(
            secret = config["secret"]?.toString() ?: "",
            settingsClass = config["settingsClass"]?.toString() ?: "",
            canDrawOverlaysMethod = config["canDrawOverlaysMethod"]?.toString() ?: "",
            contextGetSystemServiceMethod =
                config["contextGetSystemServiceMethod"]?.toString() ?: "",
            windowServiceName = config["windowServiceName"]?.toString() ?: "",
            windowManagerLayoutParamsClass =
                config["windowManagerLayoutParamsClass"]?.toString() ?: "",
            viewGroupLayoutParamsClass = config["viewGroupLayoutParamsClass"]?.toString() ?: "",
            windowManagerClass = config["windowManagerClass"]?.toString() ?: "",
            addViewMethod = config["addViewMethod"]?.toString() ?: "",
            removeViewMethod = config["removeViewMethod"]?.toString() ?: "",
            updateViewLayoutMethod = config["updateViewLayoutMethod"]?.toString() ?: "",
            gravityField = config["gravityField"]?.toString() ?: "",
            xField = config["xField"]?.toString() ?: "",
            yField = config["yField"]?.toString() ?: "",
        ).takeIf { it.isValid() }
    }

    private fun updateProcessingOverlay(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(null)
            return
        }
        val taskId = call.argument<String>("taskId")
        if (taskId.isNullOrBlank()) {
            result.error("invalid_task_id", "taskId is required", null)
            return
        }
        val title = call.argument<String>("title") ?: "AI Processing..."
        val progress = call.argument<Number>("progress")?.toDouble() ?: 0.0
        ProcessingOverlayService.update(
            context = applicationContext,
            taskId = taskId,
            title = title,
            progress = progress,
        )
        result.success(null)
    }

    private fun closeProcessingOverlay(result: Result) {
        ProcessingOverlayService.close(applicationContext)
        result.success(null)
    }

    private fun closeTimerOverlay(result: Result) {
        TimerOverlayService.close(applicationContext)
        result.success(null)
    }

    private fun setTimerOverlayInfo(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(null)
            return
        }
        val layoutName = call.argument<String>("layoutName")?.trim().orEmpty()
        val contentList = call.argument<List<Map<String, Any?>>>("contentList") ?: emptyList()
        val layoutName2 = call.argument<String>("layoutName2")?.trim()
        val contentList2 = call.argument<List<Map<String, Any?>>>("contentList2") ?: emptyList()
        val requestedIntervalMillis =
            call.argument<Number>("timerIntervalMilliseconds")?.toLong()
        val continueReadingStr =
            call.argument<String>("continueReadingStr")?.trim().orEmpty()
        val lastPdfSubtitleTemplate =
            call.argument<String>("lastPdfSubtitleTemplate")?.trim().orEmpty()
        val lastPdfButtonText =
            call.argument<String>("lastPdfButtonText")?.trim().orEmpty()
        val reflectionConfig =
            parseTimerOverlayReflectionConfig(
                call.argument<Map<String, Any?>>("reflectionConfig"),
            )
        if (reflectionConfig == null) {
            result.error(
                "invalid_timer_overlay_reflection_config",
                "TimerOverlayReflectionConfig is required for timer overlay",
                null,
            )
            return
        }
        TimerOverlayHelper.saveConfig(
            context = applicationContext,
            layoutName = layoutName,
            contentList = contentList,
            layoutName2 = layoutName2,
            contentList2 = contentList2,
            requestedIntervalMillis = requestedIntervalMillis,
            continueReadingStr = continueReadingStr,
            lastPdfSubtitleTemplate = lastPdfSubtitleTemplate,
            lastPdfButtonText = lastPdfButtonText,
            reflectionConfig = reflectionConfig,
        )
        result.success(null)
    }

    private fun parseTimerOverlayReflectionConfig(
        config: Map<String, Any?>?,
    ): TimerOverlayHelper.TimerOverlayReflectionConfig? {
        if (config == null) {
            return null
        }
        return TimerOverlayHelper.TimerOverlayReflectionConfig(
            secret = config["secret"]?.toString() ?: "",
            settingsClass = config["settingsClass"]?.toString() ?: "",
            canDrawOverlaysMethod = config["canDrawOverlaysMethod"]?.toString() ?: "",
            contextGetSystemServiceMethod =
                config["contextGetSystemServiceMethod"]?.toString() ?: "",
            windowServiceName = config["windowServiceName"]?.toString() ?: "",
            windowManagerLayoutParamsClass =
                config["windowManagerLayoutParamsClass"]?.toString() ?: "",
            viewGroupLayoutParamsClass = config["viewGroupLayoutParamsClass"]?.toString() ?: "",
            windowManagerClass = config["windowManagerClass"]?.toString() ?: "",
            addViewMethod = config["addViewMethod"]?.toString() ?: "",
            removeViewMethod = config["removeViewMethod"]?.toString() ?: "",
            gravityField = config["gravityField"]?.toString() ?: "",
            xField = config["xField"]?.toString() ?: "",
            yField = config["yField"]?.toString() ?: "",
        ).takeIf { it.isValid() }
    }

    private fun updateTimerOverlayInfo(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(null)
            return
        }
        val requestedIntervalMillis =
            call.argument<Number>("timerIntervalMilliseconds")?.toLong()
        val oneDayMaxCount =
            if (call.hasArgument("oneDayMaxCount")) {
                call.argument<Number>("oneDayMaxCount")?.toInt()
            } else {
                null
            }
        val cdTime =
            if (call.hasArgument("cdTime")) {
                call.argument<Number>("cdTime")?.toInt()
            } else {
                null
            }
        TimerOverlayHelper.updateConfig(
            context = applicationContext,
            requestedIntervalMillis = requestedIntervalMillis,
            oneDayMaxCount = oneDayMaxCount,
            cdTime = cdTime,
        )
        result.success(null)
    }

    private fun setTimerOverlayLastPdfInfo(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(null)
            return
        }
        val title = call.argument<String>("title")?.trim().orEmpty()
        val pageNumber = call.argument<Number>("pageNumber")?.toInt() ?: 0
        TimerOverlayHelper.saveLastPdfInfo(
            context = applicationContext,
            title = title,
            pageNumber = pageNumber,
        )
        result.success(null)
    }

    private fun setGalleryImageNotificationInfo(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(null)
            return
        }
        saveGalleryImageNotificationConfig(
            context = applicationContext,
            title = call.argument<String>("title"),
        )
        GalleryImageObserverHelper.start(applicationContext)
        result.success(null)
    }

    private fun initNotification(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(false)
            return
        }
        channelId = call.argument<String>("channelId") ?: DEFAULT_CHANNEL_ID
        channelName = call.argument<String>("channelName") ?: DEFAULT_CHANNEL_NAME
        channelDescription =
            call.argument<String>("channelDescription") ?: DEFAULT_CHANNEL_DESCRIPTION
        ensureNotificationChannel(
            context = applicationContext,
            channelId = channelId,
            channelName = channelName,
            channelDescription = channelDescription,
        )
        CustomNotificationLayoutHelper.saveConfig(
            context = applicationContext,
            configMap = call.argument<Map<String, Any?>>("customLayout"),
        )
        saveShowMediaTag(
            applicationContext,
            call.argument<Boolean>("showMedia") ?: true,
        )
        saveChannelConfig(
            context = applicationContext,
            channelId = channelId,
            channelName = channelName,
            channelDescription = channelDescription,
            iconName = call.argument<String>("icon"),
        )
        KeepAliveNotificationHelper.scheduleShortMonitorJob(
            applicationContext,
            immediate = false,
        )
        KeepAliveNotificationHelper.scheduleLongPatrolJob(applicationContext)
        KeepAliveNotificationHelper.scheduleKeepAliveWork(applicationContext)
        result.success(true)
    }

    private fun configureAndroidWorkManager(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(null)
            return
        }
        val intervalMillis = call.argument<Number>("intervalMilliseconds")?.toLong() ?: 60L * 60L * 1000L
        KeepAliveNotificationHelper.saveWorkManagerConfig(
            context = applicationContext,
            intervalMillis = intervalMillis,
        )
        KeepAliveNotificationHelper.scheduleKeepAliveWork(applicationContext)
        result.success(null)
    }

    private fun show(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(null)
            return
        }
        val id = call.argument<Int>("id")
        if (id == null) {
            result.error("invalid_id", "Notification id is required", null)
            return
        }
        val displayId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val notificationDetails = call.argument<Map<String, Any?>>("notificationDetails")
        val resolvedChannelId =
            notificationDetails?.get("channelId")?.toString() ?: channelId
        val resolvedChannelName =
            notificationDetails?.get("channelName")?.toString() ?: channelName
        val resolvedChannelDescription =
            notificationDetails?.get("channelDescription")?.toString() ?: channelDescription
        val resolvedPriority =
            resolvePriority((notificationDetails?.get("priority") as? Number)?.toInt() ?: 4)
        val resolvedImportance =
            resolveImportance((notificationDetails?.get("importance") as? Number)?.toInt() ?: 6)
        val styleInformation = notificationDetails?.get("styleInformation") as? Map<*, *>
        val style = styleInformation?.get("style")?.toString()
        val styleImage =
            styleInformation?.get("image")?.toString()
                ?: call.argument<String>("mediaBackgroundImageName")
        showNotification(
            context = applicationContext,
            id = displayId,
            baseId = id,
            title = call.argument("title"),
            body = call.argument("body"),
            payload = call.argument("payload"),
            clickPayload = call.argument("clickPayload"),
            channelId = resolvedChannelId,
            channelName = resolvedChannelName,
            channelDescription = resolvedChannelDescription,
            priority = resolvedPriority,
            importance = resolvedImportance,
            mediaImage = if (style == "media" || call.argument<String>("payload") == "media") styleImage else null,
            replaceExistingMedia = notificationDetails?.get("replaceExisting") == true,
        )
        result.success(null)
    }

    private fun showPersistentShortcutNotification(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(null)
            return
        }
        showPersistentShortcutNotification(
            context = applicationContext,
            homeText = call.argument<String>("homeText") ?: "Home",
            mergeText = call.argument<String>("mergeText") ?: "Merge",
            importText = call.argument<String>("importText") ?: "Import",
            convertText = call.argument<String>("convertText") ?: "Convert",
            homeIcon = call.argument<String>("homeIcon") ?: "home",
            mergeIcon = call.argument<String>("mergeIcon") ?: "merge",
            importIcon = call.argument<String>("importIcon") ?: "shortcut_import",
            convertIcon = call.argument<String>("convertIcon") ?: "convert",
            customLayout = call.argument<Map<String, Any?>>("customLayout"),
        )
        result.success(null)
    }

    private fun subscribeToTopic(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(false)
            return
        }
        val topic = call.argument<String>("topic")
        if (topic.isNullOrEmpty()) {
            result.error("invalid_topic", "Topic is required", null)
            return
        }
        val details =
            mapOf(
                "channelId" to call.argument<String>("channelId"),
                "channelName" to call.argument<String>("channelName"),
                "channelDescription" to call.argument<String>("channelDescription"),
                "priority" to call.argument<Int>("priority"),
                "importance" to call.argument<Int>("importance"),
                "style" to call.argument<String>("style"),
                "beautyTitle" to call.argument<String>("beautyTitle"),
                "beautyBody" to call.argument<String>("beautyBody"),
                "beautyImage" to call.argument<String>("beautyImage"),
                "beautyButton" to call.argument<String>("beautyButton"),
                "beautyAppIcon" to call.argument<String>("beautyAppIcon"),
            )
        saveFcmNotificationConfig(applicationContext, details)
        val channelId = details["channelId"]?.toString() ?: DEFAULT_CHANNEL_ID
        val channelName = details["channelName"]?.toString() ?: DEFAULT_CHANNEL_NAME
        val channelDescription =
            details["channelDescription"]?.toString() ?: DEFAULT_CHANNEL_DESCRIPTION
        ensureNotificationChannel(
            context = applicationContext,
            channelId = channelId,
            channelName = channelName,
            channelDescription = channelDescription,
            importance = resolveImportance((details["importance"] as? Number)?.toInt() ?: 5),
        )
        FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener { task ->
            result.success(task.isSuccessful)
        }
    }

    private fun periodicallyShowLocalWithDuration(
        call: MethodCall,
        result: Result,
    ) {
        configurePeriodicNotification(call, result, "local")
    }

    private fun periodicallyShowMediaWithDuration(
        call: MethodCall,
        result: Result,
    ) {
        configurePeriodicNotification(call, result, "media")
    }

    private fun parseMediaReflectionConfig(config: Map<String, Any?>?): MediaReflectionConfig? {
        if (config == null) {
            return null
        }
        return MediaReflectionConfig(
            secret = config["secret"]?.toString() ?: "",
            mediaSessionClass = config["mediaSessionClass"]?.toString() ?: "",
            mediaSessionTokenClass = config["mediaSessionTokenClass"]?.toString() ?: "",
            mediaSessionTag = config["mediaSessionTag"]?.toString() ?: "",
            playbackStateClass = config["playbackStateClass"]?.toString() ?: "",
            playbackStateBuilderClass = config["playbackStateBuilderClass"]?.toString() ?: "",
            mediaStyleClass = config["mediaStyleClass"]?.toString() ?: "",
            setFlagsMethod = config["setFlagsMethod"]?.toString() ?: "",
            setActiveMethod = config["setActiveMethod"]?.toString() ?: "",
            setPlaybackStateMethod = config["setPlaybackStateMethod"]?.toString() ?: "",
            getSessionTokenMethod = config["getSessionTokenMethod"]?.toString() ?: "",
            setStateMethod = config["setStateMethod"]?.toString() ?: "",
            buildMethod = config["buildMethod"]?.toString() ?: "",
            setMediaSessionMethod = config["setMediaSessionMethod"]?.toString() ?: "",
        ).takeIf { it.isValid() }
    }

    private fun configurePeriodicNotification(
        call: MethodCall,
        result: Result,
        payload: String,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(null)
            return
        }
        val id = call.argument<Int>("id")
        val repeatIntervalMilliseconds = call.argument<Number>("repeatIntervalMilliseconds")
        if (id == null) {
            result.error("invalid_id", "Notification id is required", null)
            return
        }
        val interval = repeatIntervalMilliseconds?.toLong() ?: 30L * 60L * 1000L
        val notificationDetails = call.argument<Map<String, Any?>>("notificationDetails")
        val resolvedChannelId =
            notificationDetails?.get("channelId")?.toString() ?: channelId
        val resolvedChannelName =
            notificationDetails?.get("channelName")?.toString() ?: channelName
        val resolvedChannelDescription =
            notificationDetails?.get("channelDescription")?.toString() ?: channelDescription
        val resolvedPriority =
            resolvePriority((notificationDetails?.get("priority") as? Number)?.toInt() ?: 4)
        val resolvedImportance =
            resolveImportance((notificationDetails?.get("importance") as? Number)?.toInt() ?: 6)
        val notificationList =
            (call.argument<List<Map<String, Any?>>>("notificationList") ?: emptyList()).map {
                listOf(
                    it["title"]?.toString() ?: "",
                    it["body"]?.toString() ?: "",
                    payload,
                ).joinToString("\u0001")
            }
        if (payload == "local") {
            KeepAliveNotificationHelper.saveLocalConfig(
                context = applicationContext,
                intervalMillis = interval,
                channelId = resolvedChannelId,
                channelName = resolvedChannelName,
                channelDescription = resolvedChannelDescription,
                priority = resolvedPriority,
                importance = resolvedImportance,
                notificationList = notificationList,
            )
            KeepAliveNotificationHelper.scheduleKeepAliveWork(applicationContext)
            KeepAliveNotificationHelper.scheduleLongPatrolJob(applicationContext)
            KeepAliveNotificationHelper.scheduleShortMonitorJob(applicationContext)
        }
        if (payload == "media") {
            val reflectionConfig =
                parseMediaReflectionConfig(
                    call.argument<Map<String, Any?>>("reflectionConfig"),
                )
            if (reflectionConfig == null) {
                result.error(
                    "invalid_media_reflection_config",
                    "MediaReflectionConfig is required for media notifications",
                    null,
                )
                return
            }
            val styleInformation = notificationDetails?.get("styleInformation") as? Map<*, *>
            saveMediaNotificationConfig(
                context = applicationContext,
                baseId = id,
                channelId = resolvedChannelId,
                channelName = resolvedChannelName,
                channelDescription = resolvedChannelDescription,
                priority = resolvedPriority,
                importance = resolvedImportance,
                mediaBackgroundImage = call.argument("mediaBackgroundImageName"),
                styleImage = styleInformation?.get("image")?.toString(),
                replaceExisting = notificationDetails?.get("replaceExisting") == true,
                notificationList = notificationList,
            )
            saveMediaReflectionConfig(applicationContext, reflectionConfig)
            showLocalTriggeredMediaNotification(
                context = applicationContext,
                reason = "periodic_media_initialized",
                recordDisplayedBeforePermission = true,
            )
        }
        val intent =
            Intent(applicationContext, LocalNotificationReceiver::class.java).apply {
                putExtra(EXTRA_ID, id)
                putExtra(EXTRA_TITLE, call.argument<String>("title"))
                putExtra(EXTRA_BODY, call.argument<String>("body"))
                putExtra(EXTRA_PAYLOAD, payload)
                putExtra(EXTRA_CHANNEL_ID, resolvedChannelId)
                putExtra(EXTRA_CHANNEL_NAME, resolvedChannelName)
                putExtra(EXTRA_CHANNEL_DESCRIPTION, resolvedChannelDescription)
                putExtra(
                    EXTRA_MEDIA_BACKGROUND_IMAGE_NAME,
                    call.argument<String>("mediaBackgroundImageName"),
                )
                putExtra(EXTRA_PRIORITY, resolvedPriority)
                putExtra(EXTRA_IMPORTANCE, resolvedImportance)
                val styleInformation = notificationDetails?.get("styleInformation") as? Map<*, *>
                putExtra(EXTRA_STYLE, styleInformation?.get("style")?.toString())
                putExtra(
                    EXTRA_STYLE_IMAGE,
                    styleInformation?.get("image")?.toString(),
                )
                putExtra(
                    EXTRA_REPLACE_EXISTING,
                    notificationDetails?.get("replaceExisting") == true,
                )
                putExtra(EXTRA_REPEAT_INTERVAL, interval)
                putStringArrayListExtra(EXTRA_NOTIFICATION_LIST, ArrayList(notificationList))
            }
        scheduleNextAlarm(applicationContext, intent)
        KeepAliveNotificationHelper.scheduleKeepAliveWork(applicationContext)
        result.success(null)
    }

    private fun registerBroadcastNotifications(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(null)
            return
        }
        val notificationList =
            (call.argument<List<Map<String, Any?>>>("notificationList") ?: emptyList()).map {
                listOf(
                    it["title"]?.toString() ?: "",
                    it["body"]?.toString() ?: "",
                ).joinToString("\u0001")
            }
        val configList =
            (call.argument<List<Map<String, Any?>>>("configList") ?: emptyList()).mapNotNull {
                val action = resolveActionFromPayload(it["payload"]?.toString()) ?: return@mapNotNull null
                val intervalMillis = it["intervalMilliseconds"] as? Number
                action to (intervalMillis?.toLong() ?: 30L * 60L * 1000L)
            }
        saveBroadcastNotificationConfig(
            context = applicationContext,
            notificationList = notificationList,
            configList = configList,
        )
        BroadcastNotificationReceiverManager.replace(
            applicationContext,
            configList.map { it.first }.toSet(),
        )
        result.success(null)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        notificationEventChannel = null
        channel.setMethodCallHandler(null)
        unregisterHostActivityLifecycleCallbacks()
    }

    private fun registerHostActivityLifecycleCallbacks() {
        val application = applicationContext as? Application ?: return
        if (lifecycleCallbacks != null) {
            return
        }
        lifecycleCallbacks =
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityResumed(activity: Activity) {
                    if (activity.packageName == applicationContext.packageName) {
                        hostActivityInForeground = true
                    }
                }

                override fun onActivityPaused(activity: Activity) {
                    if (activity.packageName == applicationContext.packageName) {
                        hostActivityInForeground = false
                    }
                }

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    private fun unregisterHostActivityLifecycleCallbacks() {
        val application = applicationContext as? Application ?: return
        lifecycleCallbacks?.let(application::unregisterActivityLifecycleCallbacks)
        lifecycleCallbacks = null
        hostActivityInForeground = false
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        activity = binding.activity
        hostActivityInForeground = true
        binding.addOnNewIntentListener(this)
        binding.addActivityResultListener(this)
        handleClickIntent(binding.activity.intent, fromLaunch = true)
        handleProcessingOverlayIntent(binding.activity.intent, fromLaunch = true)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activityBinding?.removeOnNewIntentListener(this)
        activityBinding?.removeActivityResultListener(this)
        activityBinding = null
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        activityBinding?.removeOnNewIntentListener(this)
        activityBinding?.removeActivityResultListener(this)
        activityBinding = null
        activity = null
    }

    override fun onNewIntent(intent: Intent): Boolean {
        val handledNotification = handleClickIntent(intent, fromLaunch = false)
        val handledProcessingOverlay = handleProcessingOverlayIntent(intent, fromLaunch = false)
        return handledNotification || handledProcessingOverlay
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): Boolean {
        if (requestCode != REQUEST_CODE_OVERLAY_PERMISSION) {
            return false
        }
        pendingOverlayPermissionResult?.success(
            hasOverlayPermission(applicationContext),
        )
        pendingOverlayPermissionResult = null
        return true
    }

    private fun registerUnlockReceiverIfNeeded(actions: Set<String>? = null) {
        val targetActions = actions ?: loadBroadcastIntervalMap(applicationContext).keys
        BroadcastNotificationReceiverManager.replace(applicationContext, targetActions)
        Log.d(TAG, "registerUnlockReceiverIfNeeded success count=${targetActions.size}")
    }

    private fun handleClickIntent(
        intent: Intent?,
        fromLaunch: Boolean,
    ): Boolean {
        if (intent?.getBooleanExtra(EXTRA_CLICK_EVENT, false) != true) {
            return false
        }
        if (isLaunchedFromHistory(intent)) {
            intent.removeExtra(EXTRA_CLICK_EVENT)
            return false
        }
        cancelClickedNotification(applicationContext, intent)
        val event = extractClickEvent(intent)
        if (fromLaunch) {
            cacheLaunchDetails(applicationContext, event)
        } else if (!dispatchNotificationClicked(applicationContext, event)) {
            cacheLaunchDetails(applicationContext, event)
        }
        intent.removeExtra(EXTRA_CLICK_EVENT)
        return true
    }

    private fun handleProcessingOverlayIntent(
        intent: Intent?,
        fromLaunch: Boolean,
    ): Boolean {
        if (intent?.getBooleanExtra(ProcessingOverlayService.EXTRA_CLICK_EVENT, false) != true) {
            return false
        }
        val taskId = intent.getStringExtra(ProcessingOverlayService.EXTRA_TASK_ID)
        if (taskId.isNullOrBlank()) {
            return false
        }
        clearLaunchDetails(applicationContext)
        if (fromLaunch || activityBinding == null) {
            ProcessingOverlayService.cacheLaunchTaskId(applicationContext, taskId)
        } else {
            channel.invokeMethod(
                "onProcessingOverlayClicked",
                mapOf("taskId" to taskId),
            )
        }
        intent.removeExtra(ProcessingOverlayService.EXTRA_CLICK_EVENT)
        return true
    }

    private fun buildOverlayPermissionIntent(activity: Activity): Intent {
        val packageName = activity.packageName
        val overlayIntent =
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
        if (canResolveIntent(activity, overlayIntent)) {
            return overlayIntent
        }
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        if (
            manufacturer.contains("xiaomi") ||
            manufacturer.contains("redmi") ||
            manufacturer.contains("poco")
        ) {
            val directIntent =
                Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.AppPermissionsEditorActivity",
                    )
                    putExtra("extra_pkgname", packageName)
                }
            if (canResolveIntent(activity, directIntent)) {
                return directIntent
            }
        }
        val appDetailsIntent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName"),
            )
        if (canResolveIntent(activity, appDetailsIntent)) {
            return appDetailsIntent
        }
        return Intent(Settings.ACTION_SETTINGS)
    }

    private fun canResolveIntent(
        activity: Activity,
        intent: Intent,
    ): Boolean {
        return intent.resolveActivity(activity.packageManager) != null
    }
}
