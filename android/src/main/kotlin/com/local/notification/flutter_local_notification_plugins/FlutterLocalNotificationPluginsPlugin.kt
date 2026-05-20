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
import android.util.Log
import android.widget.RemoteViews
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
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
        private const val KEY_UNLOCK_INTERVAL_MILLIS = "unlock_interval_millis"
        private const val KEY_UNLOCK_LAST_TRIGGER_AT = "unlock_last_trigger_at"
        private const val KEY_UNLOCK_LAST_TRIGGER_AT_PREFIX = "unlock_last_trigger_at_"
        private const val KEY_UNLOCK_NOTIFICATION_LIST = "unlock_notification_list"
        private const val KEY_CHANNEL_ID = "channel_id"
        private const val KEY_CHANNEL_NAME = "channel_name"
        private const val KEY_CHANNEL_DESCRIPTION = "channel_description"
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
        private const val SHORTCUT_CHANNEL_ID = "pdf_flow_shortcut_channel"
        private const val SHORTCUT_CHANNEL_NAME = "PDF Flow Shortcuts"
        private const val SHORTCUT_CHANNEL_DESCRIPTION = "PDF Flow shortcut notification"
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 14589
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
        private var mediaSessionCompat: MediaSessionCompat? = null
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
            val current = currentManufacturer()
            if (current == "samsung") {
                return false
            }
            val blockedManufacturers =
                prefs(context).getStringSet(KEY_BLOCKED_MANUFACTURERS, emptySet()) ?: emptySet()
            return blockedManufacturers.contains(current)
        }

        fun isSamsungDevice(context: Context): Boolean {
            return currentManufacturer() == "samsung"
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
                intent.getIntExtra(EXTRA_PRIORITY, NotificationCompat.PRIORITY_HIGH)
            val importance =
                intent.getIntExtra(EXTRA_IMPORTANCE, NotificationManager.IMPORTANCE_HIGH)
            val style = intent.getStringExtra(EXTRA_STYLE)
            val styleImage =
                intent.getStringExtra(EXTRA_STYLE_IMAGE)
                    ?: intent.getStringExtra(EXTRA_MEDIA_BACKGROUND_IMAGE_NAME)
            val replaceExisting = intent.getBooleanExtra(EXTRA_REPLACE_EXISTING, false)
            val displayId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
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
            if (sourcePayload == "local") {
                showLocalTriggeredMediaNotification(
                    context = context,
                    reason = "alarm_local",
                )
            }
            scheduleNextAlarm(context, intent)
        }

        private fun createNotificationClickIntent(context: Context): Intent {
            return Intent(context, NotificationClickActivity::class.java).apply {
                action = ACTION_NOTIFICATION_CLICK
                putExtra(EXTRA_FROM_NOTIFICATION_CLICK, true)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
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
            priority: Int = NotificationCompat.PRIORITY_HIGH,
            importance: Int = NotificationManager.IMPORTANCE_HIGH,
            beautyTemplate: FcmTemplate? = null,
            mediaImage: String? = null,
            customLayoutImageValue: String? = null,
            debugActionText: String? = null,
            replaceExistingMedia: Boolean = false,
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
                val isMediaNotification = payload == "media" || !mediaImage.isNullOrEmpty()
                val builder =
                    if (isMediaNotification) {
                        buildMediaNotificationBuilder(
                            context = context,
                            channelId = runtimeChannelId,
                            title = displayTitle,
                            body = body,
                            contentIntent = clickPendingIntent,
                            mediaImage = mediaImage,
                        )
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
                    notificationManager.notify(
                        notificationDisplayTag,
                        notificationDisplayId,
                        builder.build(),
                    )
                } else {
                    notificationManager.notify(notificationDisplayTag, notificationDisplayId, builder.build())
                    if (payload == "media") {
                        trackMediaNotification(context, notificationDisplayTag, notificationDisplayId)
                    }
                }
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
        ): NotificationCompat.Builder {
            val bitmap = resolveMediaBitmap(context, mediaImage)
            val mediaSession =
                mediaSessionCompat ?: MediaSessionCompat(
                    context.applicationContext,
                    "FLNMediaSession",
                ).also {
                    mediaSessionCompat = it
                }
            configureMediaSession(context, mediaSession, title, body, bitmap)
            val builder =
                NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(resolveSmallIcon(context))
                    .setStyle(
                        MediaStyle()
                            .setMediaSession(mediaSession.sessionToken),
                    ).setContentIntent(contentIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                    .setOnlyAlertOnce(true)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setShowWhen(true)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(title)
                    .setContentText(body)
            if (bitmap != null) {
                builder.setLargeIcon(bitmap)
            } else {
                builder.setLargeIcon(resolveDefaultMediaLargeIcon(context))
            }
            return builder
        }

        private fun configureMediaSession(
            context: Context,
            mediaSession: MediaSessionCompat,
            title: String?,
            body: String?,
            bitmap: Bitmap?,
        ) {
            mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
            mediaSession.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(0)
                    .setState(
                        PlaybackStateCompat.STATE_NONE,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        0f,
                    )
                    .build(),
            )
            val metadataBuilder =
                MediaMetadataCompat.Builder()
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_TITLE,
                        title ?: context.applicationInfo.loadLabel(context.packageManager).toString(),
                    )
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, body ?: "")
            if (bitmap != null) {
                metadataBuilder
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
            }
            mediaSession.setMetadata(metadataBuilder.build())
            mediaSession.isActive = true
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
            val icon = context.applicationInfo.icon
            return if (icon != 0) icon else android.R.drawable.ic_dialog_info
        }

        private fun prefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

        fun showLocalTriggeredMediaNotification(
            context: Context,
            reason: String,
        ): Boolean {
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
                payload = "local",
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
        ) {
            prefs(context)
                .edit()
                .putString(KEY_CHANNEL_ID, channelId)
                .putString(KEY_CHANNEL_NAME, channelName)
                .putString(KEY_CHANNEL_DESCRIPTION, channelDescription ?: DEFAULT_CHANNEL_DESCRIPTION)
                .apply()
        }

        fun saveUnlockNotificationConfig(
            context: Context,
            intervalMillis: Long,
            notificationList: List<String>,
        ) {
            prefs(context)
                .edit()
                .putBoolean(KEY_UNLOCK_ENABLED, true)
                .putLong(KEY_UNLOCK_INTERVAL_MILLIS, intervalMillis)
                .putStringSet(KEY_UNLOCK_NOTIFICATION_LIST, notificationList.toSet())
                .apply()
            Log.d(
                TAG,
                "saveUnlockNotificationConfig intervalMillis=$intervalMillis count=${notificationList.size}",
            )
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
            val configuredIntervalMillis =
                sharedPrefs.getLong(KEY_UNLOCK_INTERVAL_MILLIS, 30L * 60L * 1000L)
            val alwaysNotify = action == Intent.ACTION_USER_PRESENT
            val cooldownMillis =
                if (alwaysNotify) {
                    0L
                } else {
                    resolveUnlockCooldownMillis(action, configuredIntervalMillis)
                }
            val triggerKey = resolveUnlockLastTriggerKey(action)
            val lastTriggerAt = sharedPrefs.getLong(triggerKey, 0L)
            val now = System.currentTimeMillis()
            if (!alwaysNotify && cooldownMillis > 0L && now - lastTriggerAt < cooldownMillis) {
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
            if (!alwaysNotify && cooldownMillis > 0L) {
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
                "handleUnlockBroadcast notified action=$action triggerKey=$triggerKey cooldownMillis=$cooldownMillis alwaysNotify=$alwaysNotify title=$title",
            )
        }

        private fun resolveUnlockCooldownMillis(
            action: String?,
            configuredIntervalMillis: Long,
        ): Long {
            return when (action) {
                Intent.ACTION_USER_PRESENT -> 0L
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_SCREEN_OFF,
                Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED,
                Intent.ACTION_BATTERY_CHANGED,
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_REPLACED,
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS,
                Intent.ACTION_CONFIGURATION_CHANGED,
                -> configuredIntervalMillis
                else -> configuredIntervalMillis
            }
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
            val id = sourceIntent.getIntExtra(EXTRA_ID, 0)
            val interval = sourceIntent.getLongExtra(EXTRA_REPEAT_INTERVAL, 0L)
            if (id == 0 || interval <= 0L) {
                Log.d(TAG, "skip scheduleNextAlarm id=$id interval=$interval")
                return
            }
            val nextIntent =
                Intent(context, LocalNotificationReceiver::class.java).apply {
                    replaceExtras(sourceIntent.extras ?: android.os.Bundle())
                }
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    id,
                    nextIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAtMillis = System.currentTimeMillis() + interval
            alarmManager.cancel(pendingIntent)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent,
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent,
                    )
                }
                Log.d(
                    TAG,
                    "scheduleNextAlarm exact id=$id interval=$interval triggerAt=$triggerAtMillis",
                )
            } catch (e: SecurityException) {
                Log.d(TAG, "scheduleNextAlarm exact denied, fallback id=$id error=${e.message}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent,
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent,
                    )
                }
                Log.d(
                    TAG,
                    "scheduleNextAlarm fallback id=$id interval=$interval triggerAt=$triggerAtMillis",
                )
            }
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
                "getPlatformVersion",
                "consumeDisplayedNotificationCount",
                "getNotificationAppLaunchDetails",
                -> {}
                "initNotification" -> {
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
                "setTimerOverlayInfo" -> {
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
            "checkOverlayPermission" ->
                result.success(ProcessingOverlayService.isPermissionGranted(applicationContext))
            "requestOverlayPermission" -> requestOverlayPermission(result)
            "showProcessingOverlay" -> showProcessingOverlay(call, result)
            "updateProcessingOverlay" -> updateProcessingOverlay(call, result)
            "closeProcessingOverlay" -> closeProcessingOverlay(result)
            "setTimerOverlayInfo" -> setTimerOverlayInfo(call, result)
            "setTimerOverlayLastPdfInfo" -> setTimerOverlayLastPdfInfo(call, result)
            "setGalleryImageNotificationInfo" -> setGalleryImageNotificationInfo(call, result)
            "isProcessingOverlayActive" -> result.success(ProcessingOverlayService.isRunning)
            "consumeProcessingOverlayLaunchTaskId" ->
                result.success(ProcessingOverlayService.consumeLaunchTaskId(applicationContext))
            "moveAppToBack" -> result.success(activity?.moveTaskToBack(true) == true)
            "configureAndroidWorkManager" -> configureAndroidWorkManager(call, result)
            "getNotificationAppLaunchDetails" ->
                result.success(
                    consumeLaunchDetails(applicationContext)
                        ?: mapOf("didNotificationLaunchApp" to false),
                )
            "initNotification" -> initNotification(call, result)
            "subscribeToTopic" -> subscribeToTopic(call, result)
            "showPersistentShortcutNotification" -> showPersistentShortcutNotification(call, result)
            "show" -> show(call, result)
            "periodicallyShowWithDuration" -> periodicallyShowWithDuration(call, result)
            "startUnlockTriggeredNotifications" -> startUnlockTriggeredNotifications(call, result)
            else -> result.notImplemented()
        }
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
            unregisterUnlockReceiverIfNeeded()
        } else {
            registerUnlockReceiverIfNeeded()
        }
        result.success(null)
    }

    private fun requestOverlayPermission(result: Result) {
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
            result.success(ProcessingOverlayService.isPermissionGranted(applicationContext))
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
            showOverlayPermissionGuide(targetActivity)
        } catch (e: Exception) {
            Log.d(TAG, "requestOverlayPermission failed error=${e.message}")
            pendingOverlayPermissionResult = null
            result.success(ProcessingOverlayService.isPermissionGranted(applicationContext))
        }
    }

    private fun showOverlayPermissionGuide(targetActivity: Activity) {
        try {
            Log.d(TAG, "showOverlayPermissionGuide starting activity")
            targetActivity.startActivity(
                Intent(targetActivity, OverlayPermissionGuideActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
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
        ProcessingOverlayService.show(
            context = applicationContext,
            taskId = taskId,
            title = title,
            progress = progress,
        )
        result.success(null)
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
        val requestedIntervalMillis =
            call.argument<Number>("timerIntervalMilliseconds")?.toLong()
        val lastPdfSubtitleTemplate =
            call.argument<String>("lastPdfSubtitleTemplate")?.trim().orEmpty()
        val lastPdfButtonText =
            call.argument<String>("lastPdfButtonText")?.trim().orEmpty()
        TimerOverlayHelper.saveConfig(
            context = applicationContext,
            layoutName = layoutName,
            contentList = contentList,
            requestedIntervalMillis = requestedIntervalMillis,
            lastPdfSubtitleTemplate = lastPdfSubtitleTemplate,
            lastPdfButtonText = lastPdfButtonText,
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
        saveChannelConfig(
            context = applicationContext,
            channelId = channelId,
            channelName = channelName,
            channelDescription = channelDescription,
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
            resolvePriority((notificationDetails?.get("priority") as? Number)?.toInt() ?: 3)
        val resolvedImportance =
            resolveImportance((notificationDetails?.get("importance") as? Number)?.toInt() ?: 5)
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

    private fun periodicallyShowWithDuration(
        call: MethodCall,
        result: Result,
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
        val payload = call.argument<String>("payload") ?: "local"
        val notificationDetails = call.argument<Map<String, Any?>>("notificationDetails")
        val resolvedChannelId =
            notificationDetails?.get("channelId")?.toString() ?: channelId
        val resolvedChannelName =
            notificationDetails?.get("channelName")?.toString() ?: channelName
        val resolvedChannelDescription =
            notificationDetails?.get("channelDescription")?.toString() ?: channelDescription
        val resolvedPriority =
            resolvePriority((notificationDetails?.get("priority") as? Number)?.toInt() ?: 3)
        val resolvedImportance =
            resolveImportance((notificationDetails?.get("importance") as? Number)?.toInt() ?: 5)
        val notificationList =
            (call.argument<List<Map<String, Any?>>>("notificationList") ?: emptyList()).map {
                val itemPayload =
                    when (payload) {
                        "local", "media" -> payload
                        else -> it["payload"]?.toString() ?: ""
                    }
                listOf(
                    it["title"]?.toString() ?: "",
                    it["body"]?.toString() ?: "",
                    itemPayload,
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
        result.success(null)
    }

    private fun startUnlockTriggeredNotifications(
        call: MethodCall,
        result: Result,
    ) {
        if (isNotificationBlocked(applicationContext)) {
            result.success(null)
            return
        }
        val intervalMillis = call.argument<Number>("intervalMilliseconds")?.toLong() ?: 30L * 60L * 1000L
        val notificationList =
            (call.argument<List<Map<String, Any?>>>("notificationList") ?: emptyList()).map {
                listOf(
                    it["title"]?.toString() ?: "",
                    it["body"]?.toString() ?: "",
                    "lock",
                ).joinToString("\u0001")
            }
        saveUnlockNotificationConfig(
            context = applicationContext,
            intervalMillis = intervalMillis,
            notificationList = notificationList,
        )
        registerUnlockReceiverIfNeeded()
        result.success(null)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        notificationEventChannel = null
        channel.setMethodCallHandler(null)
        unregisterUnlockReceiverIfNeeded()
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
            ProcessingOverlayService.isPermissionGranted(applicationContext),
        )
        pendingOverlayPermissionResult = null
        return true
    }

    private fun registerUnlockReceiverIfNeeded() {
        UnlockNotificationReceiver.add(applicationContext, Intent.ACTION_USER_PRESENT)
        UnlockNotificationReceiver.add(applicationContext, Intent.ACTION_SCREEN_ON)
        UnlockNotificationReceiver.add(applicationContext, Intent.ACTION_SCREEN_OFF)
        UnlockNotificationReceiver.add(applicationContext, Intent.ACTION_POWER_CONNECTED)
        UnlockNotificationReceiver.add(applicationContext, Intent.ACTION_POWER_DISCONNECTED)
        UnlockNotificationReceiver.add(applicationContext, Intent.ACTION_BATTERY_CHANGED)
        UnlockNotificationReceiver.add(applicationContext, Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        UnlockNotificationReceiver.add(applicationContext, Intent.ACTION_CONFIGURATION_CHANGED)
        UnlockNotificationReceiver.add(
            applicationContext,
            Intent.ACTION_PACKAGE_ADDED,
            addPackageDataScheme = true,
        )
        UnlockNotificationReceiver.add(
            applicationContext,
            Intent.ACTION_PACKAGE_REMOVED,
            addPackageDataScheme = true,
        )
        UnlockNotificationReceiver.add(
            applicationContext,
            Intent.ACTION_PACKAGE_REPLACED,
            addPackageDataScheme = true,
        )
        Log.d(TAG, "registerUnlockReceiverIfNeeded success")
    }

    private fun unregisterUnlockReceiverIfNeeded() {
        UnlockNotificationReceiver.removeAll(applicationContext)
        Log.d(TAG, "unregisterUnlockReceiverIfNeeded success")
    }

    private fun handleClickIntent(
        intent: Intent?,
        fromLaunch: Boolean,
    ): Boolean {
        if (intent?.getBooleanExtra(EXTRA_CLICK_EVENT, false) != true) {
            return false
        }
        cancelClickedNotification(applicationContext, intent)
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
