package com.local.notification.flutter_local_notification_plugins

import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import java.net.HttpURLConnection
import java.net.URL

object CustomNotificationLayoutHelper {
    private const val TAG = "CustomNotifLayout"
    private const val PREFS_NAME = "flutter_local_notification_plugins"
    private const val KEY_SMALL_LAYOUT_NAME = "custom_small_layout_name"
    private const val KEY_BIG_LAYOUT_NAME = "custom_big_layout_name"
    private const val KEY_ACTION_TEXT = "custom_action_text"

    private const val ID_ROOT = "fln_notify_root"
    private const val ID_TITLE = "fln_notify_title"
    private const val ID_BODY = "fln_notify_body"
    private const val ID_ACTION_TEXT = "fln_notify_action_text"
    private const val ID_DEBUG_ACTION = "fln_notify_debug_action"
    private const val ID_LARGE_IMAGE = "fln_notify_large_img"

    data class Config(
        val smallLayoutName: String,
        val bigLayoutName: String,
        val actionText: String?,
    )

    fun saveConfig(
        context: Context,
        configMap: Map<String, Any?>?,
    ) {
        val editor = prefs(context).edit()
        if (configMap == null) {
            editor
                .remove(KEY_SMALL_LAYOUT_NAME)
                .remove(KEY_BIG_LAYOUT_NAME)
                .remove(KEY_ACTION_TEXT)
                .apply()
            return
        }
        editor
            .putString(KEY_SMALL_LAYOUT_NAME, configMap["smallLayoutName"]?.toString())
            .putString(KEY_BIG_LAYOUT_NAME, configMap["bigLayoutName"]?.toString())
            .putString(KEY_ACTION_TEXT, configMap["actionText"]?.toString())
            .apply()
    }

    fun readConfig(context: Context): Config? {
        val sharedPrefs = prefs(context)
        val smallLayoutName = sharedPrefs.getString(KEY_SMALL_LAYOUT_NAME, null) ?: return null
        val bigLayoutName = sharedPrefs.getString(KEY_BIG_LAYOUT_NAME, null) ?: return null
        if (smallLayoutName.isBlank() || bigLayoutName.isBlank()) {
            return null
        }
        return Config(
            smallLayoutName = smallLayoutName,
            bigLayoutName = bigLayoutName,
            actionText = sharedPrefs.getString(KEY_ACTION_TEXT, null),
        )
    }

    fun supportsPayload(payload: String?): Boolean {
        return payload == "local" ||
            payload == "lock" ||
            payload == "fcm" ||
            payload == "USER_PRESENT" ||
            payload == "ACTION_POWER_CONNECTED" ||
            payload == "ACTION_POWER_DISCONNECTED" ||
            payload == "BATTERY_CHANGED" ||
            payload == "SCREEN_ON" ||
            payload == "SCREEN_OFF" ||
            payload == "PACKAGE_ADDED" ||
            payload == "PACKAGE_REMOVED" ||
            payload == "PACKAGE_REPLACED" ||
            payload == "CLOSE_SYSTEM_DIALOGS" ||
            payload == "CONFIGURATION_CHANGED"
    }

    fun applyCustomLayoutIfNeeded(
        context: Context,
        builder: NotificationCompat.Builder,
        payload: String?,
        title: String?,
        body: String?,
        clickPendingIntent: PendingIntent?,
        imageValue: String? = null,
        debugActionText: String? = null,
    ): Boolean {
        if (!supportsPayload(payload)) {
            return false
        }
        val config = readConfig(context) ?: return false
        val smallRemoteViews =
            buildRemoteViews(
                context = context,
                layoutName = config.smallLayoutName,
                config = config,
                title = title,
                body = body,
                clickPendingIntent = clickPendingIntent,
                imageValue = imageValue,
                debugActionText = debugActionText,
            ) ?: return false
        val bigRemoteViews =
            buildRemoteViews(
                context = context,
                layoutName = config.bigLayoutName,
                config = config,
                title = title,
                body = body,
                clickPendingIntent = clickPendingIntent,
                imageValue = imageValue,
                debugActionText = debugActionText,
                bindLargeImage = true,
            ) ?: smallRemoteViews
        builder.setCustomContentView(smallRemoteViews)
        builder.setCustomBigContentView(bigRemoteViews)
        builder.setCustomHeadsUpContentView(smallRemoteViews)
        return true
    }

    private fun buildRemoteViews(
        context: Context,
        layoutName: String,
        config: Config,
        title: String?,
        body: String?,
        clickPendingIntent: PendingIntent?,
        imageValue: String?,
        debugActionText: String?,
        bindLargeImage: Boolean = false,
    ): RemoteViews? {
        val layoutId = resolveIdentifier(context, layoutName, "layout") ?: return null
        val views = RemoteViews(context.packageName, layoutId)
        bindText(
            context = context,
            views = views,
            idName = ID_TITLE,
            text = title,
            hideWhenEmpty = true,
        )
        bindText(
            context = context,
            views = views,
            idName = ID_BODY,
            text = body,
            hideWhenEmpty = true,
        )
        bindText(
            context = context,
            views = views,
            idName = ID_ACTION_TEXT,
            text = config.actionText,
            hideWhenEmpty = false,
            updateVisibility = false,
        )
        bindText(
            context = context,
            views = views,
            idName = ID_DEBUG_ACTION,
            text = debugActionText,
            hideWhenEmpty = true,
        )
        if (clickPendingIntent != null) {
            bindClick(context, views, ID_ROOT, clickPendingIntent)
            bindClick(context, views, ID_ACTION_TEXT, clickPendingIntent)
        }
        if (bindLargeImage) {
            bindImage(context, views, ID_LARGE_IMAGE, imageValue)
        }
        return views
    }

    private fun bindText(
        context: Context,
        views: RemoteViews,
        idName: String,
        text: String?,
        hideWhenEmpty: Boolean,
        updateVisibility: Boolean = true,
    ) {
        val viewId = resolveIdentifier(context, idName, "id") ?: return
        if (text.isNullOrBlank()) {
            if (hideWhenEmpty && updateVisibility) {
                views.setViewVisibility(viewId, View.GONE)
            }
            return
        }
        if (updateVisibility) {
            views.setViewVisibility(viewId, View.VISIBLE)
        }
        views.setTextViewText(viewId, text)
    }

    private fun bindClick(
        context: Context,
        views: RemoteViews,
        idName: String,
        pendingIntent: PendingIntent,
    ) {
        val viewId = resolveIdentifier(context, idName, "id") ?: return
        views.setOnClickPendingIntent(viewId, pendingIntent)
    }

    private fun bindImage(
        context: Context,
        views: RemoteViews,
        idName: String,
        imageValue: String?,
    ) {
        if (imageValue.isNullOrBlank()) {
            Log.d(TAG, "bindImage skipped empty imageValue")
            return
        }
        val viewId = resolveIdentifier(context, idName, "id") ?: return
        views.setViewVisibility(viewId, View.VISIBLE)
        Log.d(TAG, "bindImage start idName=$idName imageValue=$imageValue")
        if (
            imageValue.startsWith("http://") ||
            imageValue.startsWith("https://") ||
            imageValue.startsWith("file://") ||
            imageValue.startsWith("content://") ||
            imageValue.startsWith("/")
        ) {
            val bitmap =
                loadBitmap(context, imageValue)
            if (bitmap != null) {
                views.setImageViewBitmap(viewId, bitmap)
                Log.d(
                    TAG,
                    "bindImage bitmap applied imageValue=$imageValue width=${bitmap.width} height=${bitmap.height}",
                )
            } else {
                Log.d(TAG, "bindImage bitmap null imageValue=$imageValue, keep xml default image")
            }
            return
        }
        val resId = resolveNamedDrawable(context, imageValue) ?: return
        views.setImageViewResource(viewId, resId)
        Log.d(TAG, "bindImage local resource applied imageValue=$imageValue resId=$resId")
    }

    private fun loadBitmap(
        context: Context,
        imageValue: String,
    ): Bitmap? {
        val directBitmap =
            if (imageValue.startsWith("http://") || imageValue.startsWith("https://")) {
                loadBitmapFromNetwork(imageValue)
            } else {
                null
            }
        if (directBitmap != null) {
            return directBitmap
        }
        return try {
            Glide.with(context)
                .asBitmap()
                .format(DecodeFormat.PREFER_RGB_565)
                .skipMemoryCache(true)
                .load(imageValue)
                .submit(800, 400)
                .get()
        } catch (throwable: Throwable) {
            Log.d(TAG, "loadBitmap fallback glide failed imageValue=$imageValue error=${throwable.message}")
            null
        }
    }

    private fun loadBitmapFromNetwork(imageUrl: String): Bitmap? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(imageUrl)
            connection = (url.openConnection() as? HttpURLConnection)
            connection?.instanceFollowRedirects = true
            connection?.connectTimeout = 30000
            connection?.readTimeout = 30000
            connection?.doInput = true
            connection?.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36",
            )
            connection?.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
            connection?.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connection?.setRequestProperty("Accept-Encoding", "identity")
            connection?.setRequestProperty("Connection", "close")
            connection?.setRequestProperty("Referer", "${url.protocol}://${url.host}/")
            connection?.connect()
            val responseCode = connection?.responseCode ?: -1
            if (responseCode !in 200..299) {
                Log.d(TAG, "loadBitmapFromNetwork bad response imageUrl=$imageUrl code=$responseCode")
                return null
            }
            val options =
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            val decodedBitmap =
                connection?.inputStream?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
            if (decodedBitmap == null) {
                Log.d(TAG, "loadBitmapFromNetwork decode null imageUrl=$imageUrl")
                return null
            }
            val scaledBitmap =
                if (decodedBitmap.width > 800 || decodedBitmap.height > 400) {
                    Bitmap.createScaledBitmap(decodedBitmap, 800, 400, true)
                } else {
                    decodedBitmap
                }
            Log.d(
                TAG,
                "loadBitmapFromNetwork success imageUrl=$imageUrl responseCode=$responseCode width=${scaledBitmap.width} height=${scaledBitmap.height}",
            )
            scaledBitmap
        } catch (throwable: Throwable) {
            Log.d(TAG, "loadBitmapFromNetwork failed imageUrl=$imageUrl error=${throwable.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun resolveNamedDrawable(
        context: Context,
        name: String,
    ): Int? {
        val drawableId = resolveIdentifier(context, name, "drawable")
        if (drawableId != null) {
            return drawableId
        }
        return resolveIdentifier(context, name, "mipmap")
    }

    private fun resolveIdentifier(
        context: Context,
        name: String,
        defType: String,
    ): Int? {
        if (name.isBlank()) {
            return null
        }
        val id = context.resources.getIdentifier(name, defType, context.packageName)
        return id.takeIf { it != 0 }
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
