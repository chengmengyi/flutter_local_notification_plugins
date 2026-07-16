package com.local.notification.flutter_local_notification_plugins

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object GalleryImageObserverHelper {
    private const val TAG = "GalleryImageObserver"
    private const val PREFS_NAME = "flutter_local_notification_plugins"
    private const val KEY_ENABLED = "gallery_image_observer_enabled"
    private const val KEY_NOTIFICATION_TITLE = "gallery_image_notification_title"
    private const val KEY_LAST_IMAGE_ID = "gallery_image_last_id"
    private const val KEY_LAST_IMAGE_DATE_ADDED = "gallery_image_last_date_added"
    private const val KEY_LAST_NOTIFIED_IMAGE_ID = "gallery_image_last_notified_id"
    private const val QUERY_DELAY_MILLIS = 1200L

    private val handler = Handler(Looper.getMainLooper())
    private var observer: ContentObserver? = null
    private var queryRunnable: Runnable? = null

    fun saveConfig(
        context: Context,
        title: String?,
    ) {
        val normalizedTitle = title?.trim().orEmpty()
        prefs(context)
            .edit()
            .putBoolean(KEY_ENABLED, normalizedTitle.isNotEmpty())
            .putString(KEY_NOTIFICATION_TITLE, normalizedTitle)
            .apply()
        Log.d(TAG, "saveConfig enabled=${normalizedTitle.isNotEmpty()}")
    }

    fun start(context: Context) {
        val appContext = context.applicationContext
        if (!isEnabled(appContext)) {
            Log.d(TAG, "start skipped disabled")
            return
        }
        if (observer != null) {
            checkLatestImage(appContext, notifyForNewer = true, reason = "already_registered")
            return
        }
        observer =
            object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    scheduleCheck(appContext, "on_change")
                }

                override fun onChange(
                    selfChange: Boolean,
                    uri: Uri?,
                ) {
                    scheduleCheck(appContext, "on_change_uri")
                }
            }
        try {
            appContext.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer ?: return,
            )
            Log.d(TAG, "start registered")
            checkLatestImage(appContext, notifyForNewer = true, reason = "start")
        } catch (e: Exception) {
            observer = null
            Log.d(TAG, "start failed error=${e.message}")
        }
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        queryRunnable?.let { handler.removeCallbacks(it) }
        queryRunnable = null
        observer?.let {
            try {
                appContext.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                Log.d(TAG, "stop unregister failed error=${e.message}")
            }
        }
        observer = null
        Log.d(TAG, "stop")
    }

    private fun scheduleCheck(
        context: Context,
        reason: String,
    ) {
        queryRunnable?.let { handler.removeCallbacks(it) }
        val appContext = context.applicationContext
        queryRunnable =
            Runnable {
                queryRunnable = null
                try {
                    checkLatestImage(appContext, notifyForNewer = true, reason = reason)
                } catch (e: Exception) {
                    Log.e(TAG, "delayed check failed reason=$reason", e)
                }
            }
        handler.postDelayed(queryRunnable ?: return, QUERY_DELAY_MILLIS)
    }

    private fun checkLatestImage(
        context: Context,
        notifyForNewer: Boolean,
        reason: String,
    ) {
        if (!isEnabled(context)) {
            Log.d(TAG, "check skipped disabled reason=$reason")
            return
        }
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
            Log.d(TAG, "check skipped notification blocked reason=$reason")
            return
        }
        if (!canPostNotifications(context)) {
            Log.d(TAG, "check skipped no notification permission reason=$reason")
            return
        }
        if (!canReadImages(context)) {
            Log.d(TAG, "check skipped no image permission reason=$reason")
            return
        }
        val latestImage = queryLatestImage(context) ?: run {
            Log.d(TAG, "check skipped empty latest reason=$reason")
            return
        }
        val sharedPrefs = prefs(context)
        val lastId = sharedPrefs.getLong(KEY_LAST_IMAGE_ID, -1L)
        val lastDateAdded = sharedPrefs.getLong(KEY_LAST_IMAGE_DATE_ADDED, 0L)
        if (lastId < 0L) {
            sharedPrefs
                .edit()
                .putLong(KEY_LAST_IMAGE_ID, latestImage.id)
                .putLong(KEY_LAST_IMAGE_DATE_ADDED, latestImage.dateAdded)
                .apply()
            Log.d(TAG, "check baseline id=${latestImage.id} reason=$reason")
            return
        }
        val isNewer =
            latestImage.dateAdded > lastDateAdded ||
                (latestImage.dateAdded == lastDateAdded && latestImage.id > lastId)
        if (!isNewer) {
            Log.d(TAG, "check no newer id=${latestImage.id} reason=$reason")
            return
        }
        sharedPrefs
            .edit()
            .putLong(KEY_LAST_IMAGE_ID, latestImage.id)
            .putLong(KEY_LAST_IMAGE_DATE_ADDED, latestImage.dateAdded)
            .apply()
        if (!notifyForNewer) {
            return
        }
        val lastNotifiedId = sharedPrefs.getLong(KEY_LAST_NOTIFIED_IMAGE_ID, -1L)
        if (lastNotifiedId == latestImage.id) {
            Log.d(TAG, "check duplicate notify id=${latestImage.id} reason=$reason")
            return
        }
        sharedPrefs.edit().putLong(KEY_LAST_NOTIFIED_IMAGE_ID, latestImage.id).apply()
        FlutterLocalNotificationPluginsPlugin.showGalleryImageNotification(context)
        Log.d(TAG, "check notified id=${latestImage.id} reason=$reason")
    }

    private fun queryLatestImage(context: Context): GalleryImage? {
        val projection =
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED,
            )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"
        return try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    null
                } else {
                    val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dateAddedIndex =
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    GalleryImage(
                        id = cursor.getLong(idIndex),
                        dateAdded = cursor.getLong(dateAddedIndex),
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "queryLatestImage failed error=${e.message}")
            null
        }
    }

    private fun isEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ENABLED, false)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun canReadImages(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager()
        ) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES,
            ) == PackageManager.PERMISSION_GRANTED
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private data class GalleryImage(
        val id: Long,
        val dateAdded: Long,
    )
}
