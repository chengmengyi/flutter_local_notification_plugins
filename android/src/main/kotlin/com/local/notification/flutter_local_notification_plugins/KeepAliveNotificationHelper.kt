package com.local.notification.flutter_local_notification_plugins

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.PersistableBundle
import android.os.PowerManager
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object KeepAliveNotificationHelper {
    private const val TAG = "LocalNotificationKeepAlive"
    private const val PREFS_NAME = "flutter_local_notification_plugins"
    private const val KEY_DISPLAYED_NOTIFICATION_COUNT = "displayed_notification_count"
    private const val KEY_SHORTCUT_HOME_TEXT = "keep_alive_shortcut_home_text"
    private const val KEY_SHORTCUT_MERGE_TEXT = "keep_alive_shortcut_merge_text"
    private const val KEY_SHORTCUT_IMPORT_TEXT = "keep_alive_shortcut_import_text"
    private const val KEY_SHORTCUT_CONVERT_TEXT = "keep_alive_shortcut_convert_text"
    private const val KEY_SHORTCUT_HOME_ICON = "keep_alive_shortcut_home_icon"
    private const val KEY_SHORTCUT_MERGE_ICON = "keep_alive_shortcut_merge_icon"
    private const val KEY_SHORTCUT_IMPORT_ICON = "keep_alive_shortcut_import_icon"
    private const val KEY_SHORTCUT_CONVERT_ICON = "keep_alive_shortcut_convert_icon"
    private const val KEY_SHORTCUT_SMALL_LAYOUT_NAME = "keep_alive_shortcut_small_layout_name"
    private const val KEY_SHORTCUT_BIG_LAYOUT_NAME = "keep_alive_shortcut_big_layout_name"
    private const val KEY_LOCAL_ENABLED = "keep_alive_local_enabled"
    private const val KEY_LOCAL_INTERVAL_MILLIS = "keep_alive_local_interval_millis"
    private const val KEY_LOCAL_CHANNEL_ID = "keep_alive_local_channel_id"
    private const val KEY_LOCAL_CHANNEL_NAME = "keep_alive_local_channel_name"
    private const val KEY_LOCAL_CHANNEL_DESCRIPTION = "keep_alive_local_channel_description"
    private const val KEY_LOCAL_PRIORITY = "keep_alive_local_priority"
    private const val KEY_LOCAL_IMPORTANCE = "keep_alive_local_importance"
    private const val KEY_LOCAL_NOTIFICATION_LIST = "keep_alive_local_notification_list"
    private const val KEY_WORK_MANAGER_INTERVAL_MILLIS = "keep_alive_work_manager_interval_millis"
    private const val DEFAULT_CHANNEL_ID = "default_notification_channel"
    private const val DEFAULT_CHANNEL_NAME = "Notifications"
    private const val DEFAULT_CHANNEL_DESCRIPTION = "App notifications"
    private const val EXTRA_ID = "id"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_BODY = "body"
    private const val EXTRA_PAYLOAD = "payload"
    private const val EXTRA_CLICK_EVENT = "flutter_local_notification_click_event"
    private const val EXTRA_FROM_NOTIFICATION_CLICK = "b03pdf.extra.FROM_NOTIFICATION_CLICK"
    private const val ACTION_NOTIFICATION_CLICK =
        "com.local.notification.flutter_local_notification_plugins.NOTIFICATION_CLICK"
    private const val SHORTCUT_NOTIFICATION_ID = 10004
    private const val SHORTCUT_CHANNEL_ID = "pdf_flow_shortcut_channel"
    private const val SHORTCUT_CHANNEL_NAME = "PDF Flow Shortcuts"
    private const val SHORTCUT_CHANNEL_DESCRIPTION = "PDF Flow shortcut notification"
    private const val PAYLOAD_SHORTCUT_HOME = "shortcut_home"
    private const val PAYLOAD_SHORTCUT_MERGE = "shortcut_merge"
    private const val PAYLOAD_SHORTCUT_IMPORT = "shortcut_import"
    private const val PAYLOAD_SHORTCUT_CONVERT = "shortcut_convert"
    private const val PAYLOAD_LOCAL = "local"
    private const val WORK_NAME_ONETIME = "pdf_flow_keep_alive_one_time_work"
    private const val WORK_NAME_PERIODIC = "pdf_flow_keep_alive_periodic_work"
    private const val JOB_MODE = "job_mode"
    private const val JOB_MODE_MONITOR = "monitor"
    private const val JOB_MODE_PATROL = "patrol"
    private const val MONITOR_JOB_ID = 421001
    private const val PATROL_JOB_ID = 421002
    private const val RESTART_REQUEST_CODE = 421003
    private const val RESTART_ACTION = "com.local.notification.flutter_local_notification_plugins.KEEP_ALIVE_RESTART"
    private const val RESTART_REASON = "restart_reason"
    private const val DEFAULT_WORK_INTERVAL_MILLIS = 60L * 60L * 1000L
    private const val RELEASE_PATROL_INTERVAL_MILLIS = 60L * 60L * 1000L
    private const val RELEASE_MONITOR_INTERVAL_MILLIS = 7_000L
    private const val MAX_ACTIVE_NOTIFICATIONS_BEFORE_POST = 22

    data class ShortcutConfig(
        val homeText: String,
        val mergeText: String,
        val importText: String,
        val convertText: String,
        val homeIcon: String,
        val mergeIcon: String,
        val importIcon: String,
        val convertIcon: String,
        val smallLayoutName: String?,
        val bigLayoutName: String?,
    )

    data class LocalConfig(
        val intervalMillis: Long,
        val channelId: String,
        val channelName: String,
        val channelDescription: String,
        val priority: Int,
        val importance: Int,
        val notificationList: List<String>,
    )

    fun saveShortcutConfig(
        context: Context,
        homeText: String,
        mergeText: String,
        importText: String,
        convertText: String,
        homeIcon: String,
        mergeIcon: String,
        importIcon: String,
        convertIcon: String,
        smallLayoutName: String?,
        bigLayoutName: String?,
    ) {
        prefs(context)
            .edit()
            .putString(KEY_SHORTCUT_HOME_TEXT, homeText)
            .putString(KEY_SHORTCUT_MERGE_TEXT, mergeText)
            .putString(KEY_SHORTCUT_IMPORT_TEXT, importText)
            .putString(KEY_SHORTCUT_CONVERT_TEXT, convertText)
            .putString(KEY_SHORTCUT_HOME_ICON, homeIcon)
            .putString(KEY_SHORTCUT_MERGE_ICON, mergeIcon)
            .putString(KEY_SHORTCUT_IMPORT_ICON, importIcon)
            .putString(KEY_SHORTCUT_CONVERT_ICON, convertIcon)
            .putString(KEY_SHORTCUT_SMALL_LAYOUT_NAME, smallLayoutName)
            .putString(KEY_SHORTCUT_BIG_LAYOUT_NAME, bigLayoutName)
            .apply()
        Log.d(TAG, "saveShortcutConfig success")
    }

    fun readShortcutConfig(context: Context): ShortcutConfig? {
        val sharedPrefs = prefs(context)
        val homeText = sharedPrefs.getString(KEY_SHORTCUT_HOME_TEXT, null) ?: return null
        val mergeText = sharedPrefs.getString(KEY_SHORTCUT_MERGE_TEXT, null) ?: return null
        val importText = sharedPrefs.getString(KEY_SHORTCUT_IMPORT_TEXT, null) ?: return null
        val convertText = sharedPrefs.getString(KEY_SHORTCUT_CONVERT_TEXT, null) ?: return null
        return ShortcutConfig(
            homeText = homeText,
            mergeText = mergeText,
            importText = importText,
            convertText = convertText,
            homeIcon = sharedPrefs.getString(KEY_SHORTCUT_HOME_ICON, "home") ?: "home",
            mergeIcon = sharedPrefs.getString(KEY_SHORTCUT_MERGE_ICON, "merge") ?: "merge",
            importIcon =
                sharedPrefs.getString(KEY_SHORTCUT_IMPORT_ICON, "shortcut_import")
                    ?: "shortcut_import",
            convertIcon = sharedPrefs.getString(KEY_SHORTCUT_CONVERT_ICON, "convert")
                ?: "convert",
            smallLayoutName = sharedPrefs.getString(KEY_SHORTCUT_SMALL_LAYOUT_NAME, null),
            bigLayoutName = sharedPrefs.getString(KEY_SHORTCUT_BIG_LAYOUT_NAME, null),
        )
    }

    fun saveLocalConfig(
        context: Context,
        intervalMillis: Long,
        channelId: String,
        channelName: String,
        channelDescription: String,
        priority: Int,
        importance: Int,
        notificationList: List<String>,
    ) {
        prefs(context)
            .edit()
            .putBoolean(KEY_LOCAL_ENABLED, true)
            .putLong(KEY_LOCAL_INTERVAL_MILLIS, intervalMillis)
            .putString(KEY_LOCAL_CHANNEL_ID, channelId)
            .putString(KEY_LOCAL_CHANNEL_NAME, channelName)
            .putString(KEY_LOCAL_CHANNEL_DESCRIPTION, channelDescription)
            .putInt(KEY_LOCAL_PRIORITY, priority)
            .putInt(KEY_LOCAL_IMPORTANCE, importance)
            .putStringSet(KEY_LOCAL_NOTIFICATION_LIST, notificationList.toSet())
            .apply()
        Log.d(
            TAG,
            "saveLocalConfig intervalMillis=$intervalMillis count=${notificationList.size}",
        )
    }

    fun saveWorkManagerConfig(
        context: Context,
        intervalMillis: Long,
    ) {
        prefs(context)
            .edit()
            .putLong(KEY_WORK_MANAGER_INTERVAL_MILLIS, intervalMillis)
            .apply()
        Log.d(TAG, "saveWorkManagerConfig intervalMillis=$intervalMillis")
    }

    fun readWorkManagerIntervalMillis(context: Context): Long {
        return prefs(context).getLong(
            KEY_WORK_MANAGER_INTERVAL_MILLIS,
            DEFAULT_WORK_INTERVAL_MILLIS,
        )
    }

    fun readLocalConfig(context: Context): LocalConfig? {
        val sharedPrefs = prefs(context)
        if (!sharedPrefs.getBoolean(KEY_LOCAL_ENABLED, false)) {
            return null
        }
        return LocalConfig(
            intervalMillis = sharedPrefs.getLong(KEY_LOCAL_INTERVAL_MILLIS, 0L),
            channelId = sharedPrefs.getString(KEY_LOCAL_CHANNEL_ID, DEFAULT_CHANNEL_ID)
                ?: DEFAULT_CHANNEL_ID,
            channelName = sharedPrefs.getString(KEY_LOCAL_CHANNEL_NAME, DEFAULT_CHANNEL_NAME)
                ?: DEFAULT_CHANNEL_NAME,
            channelDescription =
                sharedPrefs.getString(KEY_LOCAL_CHANNEL_DESCRIPTION, DEFAULT_CHANNEL_DESCRIPTION)
                    ?: DEFAULT_CHANNEL_DESCRIPTION,
            priority = sharedPrefs.getInt(KEY_LOCAL_PRIORITY, NotificationCompat.PRIORITY_HIGH),
            importance = sharedPrefs.getInt(KEY_LOCAL_IMPORTANCE, NotificationManager.IMPORTANCE_HIGH),
            notificationList =
                sharedPrefs.getStringSet(KEY_LOCAL_NOTIFICATION_LIST, emptySet())?.toList()
                    ?: emptyList(),
        )
    }

    fun showPersistentShortcutNotification(context: Context): Boolean {
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
            Log.d(TAG, "showPersistentShortcutNotification blocked by manufacturer")
            return false
        }
        return try {
            trimActiveNotificationsIfNeeded(
                context = context,
                targetCount = MAX_ACTIVE_NOTIFICATIONS_BEFORE_POST,
                reason = "persistent_shortcut",
            )
            val notification = buildPersistentShortcutNotification(context) ?: return false
            NotificationManagerCompat.from(context).notify(
                SHORTCUT_NOTIFICATION_ID,
                notification,
            )
            startOrUpdateForegroundService(context, "showPersistentShortcutNotification")
            scheduleShortMonitorJob(context, immediate = true)
            scheduleLongPatrolJob(context)
            scheduleKeepAliveWork(context)
            Log.d(TAG, "showPersistentShortcutNotification success")
            true
        } catch (e: Exception) {
            Log.d(TAG, "showPersistentShortcutNotification failed error=${e.message}")
            false
        }
    }

    fun buildPersistentShortcutNotification(context: Context): Notification? {
        val config = readShortcutConfig(context) ?: return null
        ensureNotificationChannel(
            context = context,
            channelId = SHORTCUT_CHANNEL_ID,
            channelName = SHORTCUT_CHANNEL_NAME,
            channelDescription = SHORTCUT_CHANNEL_DESCRIPTION,
            importance = NotificationManager.IMPORTANCE_LOW,
        )
        val homePendingIntent =
            createShortcutClickPendingIntent(
                context = context,
                requestCode = PAYLOAD_SHORTCUT_HOME.hashCode(),
                payload = PAYLOAD_SHORTCUT_HOME,
                title = config.homeText,
            )
        val smallViews =
            buildShortcutRemoteViews(
                context = context,
                config = config,
                customLayoutName = config.smallLayoutName,
                defaultLayoutResId = R.layout.fln_shortcut_notification_small,
                homePendingIntent = homePendingIntent,
            ) ?: return null
        val bigViews =
            buildShortcutRemoteViews(
                context = context,
                config = config,
                customLayoutName = config.bigLayoutName ?: config.smallLayoutName,
                defaultLayoutResId = R.layout.fln_shortcut_notification_big,
                homePendingIntent = homePendingIntent,
            ) ?: smallViews
        return NotificationCompat.Builder(context, SHORTCUT_CHANNEL_ID)
            .setSmallIcon(resolveSmallIcon(context))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(homePendingIntent)
            .setCustomContentView(smallViews)
            .setCustomBigContentView(bigViews)
            .setCustomHeadsUpContentView(smallViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun buildShortcutRemoteViews(
        context: Context,
        config: ShortcutConfig,
        customLayoutName: String?,
        defaultLayoutResId: Int,
        homePendingIntent: PendingIntent?,
    ): RemoteViews? {
        val customLayoutId = resolveLayoutId(context, customLayoutName)
        val views =
            if (customLayoutId != null) {
                RemoteViews(context.packageName, customLayoutId)
            } else {
                RemoteViews(context.packageName, defaultLayoutResId)
            }
        bindShortcutIcon(context, views, "fln_shortcut_logo_icon", "logo")
        bindShortcutText(context, views, "fln_shortcut_home_text", config.homeText)
        bindShortcutText(context, views, "fln_shortcut_merge_text", config.mergeText)
        bindShortcutText(context, views, "fln_shortcut_import_text", config.importText)
        bindShortcutText(context, views, "fln_shortcut_convert_text", config.convertText)
        if (customLayoutId == null) {
            bindShortcutIcon(context, views, "fln_shortcut_home_icon", config.homeIcon)
            bindShortcutIcon(context, views, "fln_shortcut_merge_icon", config.mergeIcon)
            bindShortcutIcon(context, views, "fln_shortcut_import_icon", config.importIcon)
            bindShortcutIcon(context, views, "fln_shortcut_convert_icon", config.convertIcon)
        }
        homePendingIntent?.let {
            bindShortcutClick(context, views, "fln_shortcut_root", it)
            bindShortcutClick(context, views, "fln_shortcut_home_action", it)
        }
        createShortcutClickPendingIntent(
            context = context,
            requestCode = PAYLOAD_SHORTCUT_MERGE.hashCode(),
            payload = PAYLOAD_SHORTCUT_MERGE,
            title = config.mergeText,
        )?.let {
            bindShortcutClick(context, views, "fln_shortcut_merge_action", it)
        }
        createShortcutClickPendingIntent(
            context = context,
            requestCode = PAYLOAD_SHORTCUT_IMPORT.hashCode(),
            payload = PAYLOAD_SHORTCUT_IMPORT,
            title = config.importText,
        )?.let {
            bindShortcutClick(context, views, "fln_shortcut_import_action", it)
        }
        createShortcutClickPendingIntent(
            context = context,
            requestCode = PAYLOAD_SHORTCUT_CONVERT.hashCode(),
            payload = PAYLOAD_SHORTCUT_CONVERT,
            title = config.convertText,
        )?.let {
            bindShortcutClick(context, views, "fln_shortcut_convert_action", it)
        }
        return views
    }

    private fun bindShortcutText(
        context: Context,
        views: RemoteViews,
        idName: String,
        text: String,
    ) {
        val viewId = resolveId(context, idName) ?: return
        views.setTextViewText(viewId, text)
    }

    private fun bindShortcutIcon(
        context: Context,
        views: RemoteViews,
        idName: String,
        iconName: String,
    ) {
        val viewId = resolveId(context, idName) ?: return
        val resId = resolveNamedResourceId(context, iconName) ?: return
        views.setImageViewResource(viewId, resId)
    }

    private fun bindShortcutClick(
        context: Context,
        views: RemoteViews,
        idName: String,
        pendingIntent: PendingIntent,
    ) {
        val viewId = resolveId(context, idName) ?: return
        views.setOnClickPendingIntent(viewId, pendingIntent)
    }

    private fun resolveLayoutId(
        context: Context,
        layoutName: String?,
    ): Int? {
        if (layoutName.isNullOrBlank()) {
            return null
        }
        val layoutId = context.resources.getIdentifier(layoutName, "layout", context.packageName)
        return layoutId.takeIf { it != 0 }
    }

    fun startOrUpdateForegroundService(
        context: Context,
        reason: String,
    ): Boolean {
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
            Log.d(TAG, "startOrUpdateForegroundService blocked by manufacturer")
            return false
        }
        if (readShortcutConfig(context) == null) {
            Log.d(TAG, "startOrUpdateForegroundService skipped, shortcut config empty")
            return false
        }
        return try {
            val intent =
                Intent(context, KeepAliveForegroundService::class.java).apply {
                    putExtra(RESTART_REASON, reason)
                }
            ContextCompat.startForegroundService(context, intent)
            Log.d(TAG, "startOrUpdateForegroundService success reason=$reason")
            true
        } catch (e: Exception) {
            Log.d(TAG, "startOrUpdateForegroundService failed reason=$reason error=${e.message}")
            false
        }
    }

    fun ensureForegroundServiceAlive(
        context: Context,
        reason: String,
    ): Boolean {
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
            Log.d(TAG, "ensureForegroundServiceAlive blocked by manufacturer")
            return false
        }
        return if (isPersistentShortcutNotificationActive(context)) {
            Log.d(TAG, "ensureForegroundServiceAlive active reason=$reason")
            true
        } else {
            Log.d(TAG, "ensureForegroundServiceAlive restart reason=$reason")
            showPersistentShortcutNotification(context)
        }
    }

    fun isPersistentShortcutNotificationActive(context: Context): Boolean {
        return try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.activeNotifications.any { it.id == SHORTCUT_NOTIFICATION_ID }
        } catch (e: Exception) {
            Log.d(TAG, "isPersistentShortcutNotificationActive failed error=${e.message}")
            false
        }
    }

    fun scheduleKeepAliveWork(context: Context) {
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
            return
        }
        if (readShortcutConfig(context) == null && readLocalConfig(context) == null) {
            return
        }
        val intervalMillis = readWorkManagerIntervalMillis(context)
        if (intervalMillis <= 0L) {
            return
        }
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_NAME_ONETIME)
        workManager.cancelUniqueWork(WORK_NAME_PERIODIC)
        if (intervalMillis < 15L * 60L * 1000L) {
            val request =
                OneTimeWorkRequestBuilder<LocalKeepAliveWorker>()
                    .setInitialDelay(intervalMillis, TimeUnit.MILLISECONDS)
                    .build()
            workManager.enqueueUniqueWork(
                WORK_NAME_ONETIME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
            Log.d(TAG, "scheduleKeepAliveWork oneTime interval=$intervalMillis")
        } else {
            val request =
                PeriodicWorkRequestBuilder<LocalKeepAliveWorker>(
                    intervalMillis,
                    TimeUnit.MILLISECONDS,
                    15L,
                    MINUTES,
                ).build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
            Log.d(TAG, "scheduleKeepAliveWork periodic interval=$intervalMillis")
        }
    }

    fun scheduleShortMonitorJob(
        context: Context,
        immediate: Boolean = false,
    ) {
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
            return
        }
        if (readShortcutConfig(context) == null) {
            return
        }
        val delayMillis =
            if (immediate) {
                1_000L
            } else {
                RELEASE_MONITOR_INTERVAL_MILLIS
            }
        scheduleJob(
            context = context,
            jobId = MONITOR_JOB_ID,
            mode = JOB_MODE_MONITOR,
            delayMillis = delayMillis,
        )
    }

    fun scheduleLongPatrolJob(context: Context) {
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
            return
        }
        if (readShortcutConfig(context) == null) {
            return
        }
        scheduleJob(
            context = context,
            jobId = PATROL_JOB_ID,
            mode = JOB_MODE_PATROL,
            delayMillis = RELEASE_PATROL_INTERVAL_MILLIS,
        )
    }

    fun handleJob(
        context: Context,
        mode: String,
    ) {
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
            Log.d(TAG, "handleJob blocked by manufacturer mode=$mode")
            return
        }
        when (mode) {
            JOB_MODE_MONITOR -> {
                if (!isPersistentShortcutNotificationActive(context)) {
                    val serviceStarted =
                        startOrUpdateForegroundService(context, "job_monitor")
                    if (!serviceStarted) {
                        showPersistentShortcutNotification(context)
                    }
                }
                scheduleShortMonitorJob(context)
            }

            JOB_MODE_PATROL -> {
                ensureForegroundServiceAlive(context, "job_patrol")
                scheduleLongPatrolJob(context)
            }
        }
    }

    fun showStoredLocalNotification(
        context: Context,
        source: String,
    ): Boolean {
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
            Log.d(TAG, "showStoredLocalNotification blocked by manufacturer source=$source")
            return false
        }
        val config = readLocalConfig(context) ?: return false
        if (config.notificationList.isEmpty()) {
            Log.d(TAG, "showStoredLocalNotification skipped empty source=$source")
            return false
        }
        val raw = config.notificationList[Random.nextInt(config.notificationList.size)]
        val parts = raw.split("\u0001")
        val title = parts.getOrNull(0)
        val body = parts.getOrNull(1)
        val payload = PAYLOAD_LOCAL
        try {
            prepareForDynamicNotification(
                context = context,
                reason = "keep_alive_local_$source",
            )
            val runtimeChannelId =
                buildRuntimeChannelId(
                    baseChannelId = config.channelId,
                    payload = payload,
                    title = title,
                    body = body,
                )
            ensureNotificationChannel(
                context = context,
                channelId = runtimeChannelId,
                channelName = config.channelName,
                channelDescription = config.channelDescription,
                importance = config.importance,
            )
            val displayTitle =
                resolveKeepAliveDebugDisplayTitle(
                    context = context,
                    title = title,
                    payload = payload,
                    source = source,
                )
            val displayId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            val clickIntent =
                createNotificationClickIntent(context).apply {
                    putExtra(EXTRA_CLICK_EVENT, true)
                    putExtra(EXTRA_ID, displayId)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_BODY, body)
                    putExtra(EXTRA_PAYLOAD, payload)
                }
            val clickPendingIntent =
                clickIntent.let {
                    PendingIntent.getActivity(
                        context,
                        displayId,
                        it,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                }
            val builder =
                NotificationCompat.Builder(context, runtimeChannelId)
                    .setSmallIcon(resolveSmallIcon(context))
                    .setContentTitle(displayTitle)
                    .setContentText(body)
                    .setPriority(config.priority)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setVibrate(longArrayOf(0, 180, 120, 180))
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(false)
                    .setShowWhen(true)
                    .setWhen(System.currentTimeMillis())
                    .setExtras(android.os.Bundle().apply { putString(EXTRA_PAYLOAD, payload) })
                    .setContentIntent(clickPendingIntent)
            val appliedCustomLayout =
                CustomNotificationLayoutHelper.applyCustomLayoutIfNeeded(
                    context = context,
                    builder = builder,
                    payload = payload,
                    title = displayTitle,
                    body = body,
                    clickPendingIntent = clickPendingIntent,
                )
            if (!appliedCustomLayout) {
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
            }
            NotificationManagerCompat.from(context).notify(
                "keep_alive_local_${System.currentTimeMillis()}_${Random.nextInt(1000)}",
                displayId,
                builder.build(),
            )
            FlutterLocalNotificationPluginsPlugin.dispatchNotificationDisplayed(
                context,
                mapOf(
                    "id" to displayId,
                    "title" to title,
                    "body" to body,
                    "payload" to payload,
                ),
            )
            FlutterLocalNotificationPluginsPlugin.showLocalTriggeredMediaNotification(
                context = context,
                reason = "work_manager_$source",
            )
            wakeScreenIfNeeded(context)
            Log.d(TAG, "showStoredLocalNotification success source=$source title=$title")
            return true
        } catch (e: Exception) {
            Log.d(TAG, "showStoredLocalNotification failed source=$source error=${e.message}")
            return false
        }
    }

    fun prepareForDynamicNotification(
        context: Context,
        reason: String,
    ) {
        val persistentActive = isPersistentShortcutNotificationActive(context)
        val targetCount =
            if (persistentActive) {
                MAX_ACTIVE_NOTIFICATIONS_BEFORE_POST
            } else {
                MAX_ACTIVE_NOTIFICATIONS_BEFORE_POST - 1
            }
        trimActiveNotificationsIfNeeded(
            context = context,
            targetCount = targetCount,
            reason = reason,
        )
    }

    fun buildRuntimeChannelId(
        baseChannelId: String,
        payload: String?,
        title: String?,
        body: String?,
    ): String {
        val safePayload =
            payload
                ?.trim()
                ?.lowercase()
                ?.replace(Regex("[^a-z0-9_]+"), "_")
                ?.ifBlank { "notification" }
                ?: "notification"
        val normalizedTitle = title?.trim().orEmpty()
        val normalizedBody = body?.trim().orEmpty()
        val contentHash =
            "$safePayload\u0001$normalizedTitle\u0001$normalizedBody"
                .hashCode()
                .toUInt()
                .toString(16)
        return "${baseChannelId}_${safePayload}_$contentHash"
    }

    fun scheduleRestartFallback(
        context: Context,
        reason: String,
    ) {
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
            return
        }
        if (readShortcutConfig(context) == null) {
            return
        }
        val intent =
            Intent(context, KeepAliveRestartReceiver::class.java).apply {
                action = RESTART_ACTION
                putExtra(RESTART_REASON, reason)
            }
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                RESTART_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = System.currentTimeMillis() + 2_000L
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            Log.d(TAG, "scheduleRestartFallback success reason=$reason triggerAt=$triggerAt")
        } catch (e: Exception) {
            Log.d(TAG, "scheduleRestartFallback failed reason=$reason error=${e.message}")
        }
    }

    fun handleRestartReceiver(
        context: Context,
        reason: String?,
    ) {
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
            Log.d(TAG, "handleRestartReceiver blocked by manufacturer reason=$reason")
            return
        }
        Log.d(TAG, "handleRestartReceiver reason=$reason")
        startOrUpdateForegroundService(context, reason ?: "restart_receiver")
        scheduleShortMonitorJob(context, immediate = true)
        scheduleLongPatrolJob(context)
        scheduleKeepAliveWork(context)
    }

    fun disableAllNotificationSchedulers(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_ONETIME)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
        val jobScheduler =
            context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
        jobScheduler?.cancel(MONITOR_JOB_ID)
        jobScheduler?.cancel(PATROL_JOB_ID)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val restartIntent =
            Intent(context, KeepAliveRestartReceiver::class.java).apply {
                action = RESTART_ACTION
            }
        val restartPendingIntent =
            PendingIntent.getBroadcast(
                context,
                RESTART_REQUEST_CODE,
                restartIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        alarmManager?.cancel(restartPendingIntent)
        context.stopService(Intent(context, KeepAliveForegroundService::class.java))
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun scheduleJob(
        context: Context,
        jobId: Int,
        mode: String,
        delayMillis: Long,
    ) {
        val jobScheduler =
            context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
        val componentName = ComponentName(context, LocalKeepAliveJobService::class.java)
        val extras =
            PersistableBundle().apply {
                putString(JOB_MODE, mode)
            }
        val builder =
            JobInfo.Builder(jobId, componentName)
                .setExtras(extras)
                .setMinimumLatency(delayMillis)
                .setOverrideDeadline(delayMillis + 3_000L)
                .setPersisted(false)
        val result = jobScheduler.schedule(builder.build())
        Log.d(TAG, "scheduleJob mode=$mode delay=$delayMillis result=$result")
    }

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

    private fun ensureNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String?,
        importance: Int,
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
            NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
        notificationManager.createNotificationChannel(channel)
    }

    private fun trimActiveNotificationsIfNeeded(
        context: Context,
        targetCount: Int,
        reason: String,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        if (targetCount < 0) {
            return
        }
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val activeNotifications = notificationManager.activeNotifications ?: return
            if (activeNotifications.size <= targetCount) {
                return
            }
            val cancelCandidates =
                activeNotifications
                    .filterNot { it.id == SHORTCUT_NOTIFICATION_ID }
                    .sortedBy { it.postTime }
            if (cancelCandidates.isEmpty()) {
                return
            }
            val compat = NotificationManagerCompat.from(context)
            var currentCount = activeNotifications.size
            for (candidate in cancelCandidates) {
                if (currentCount <= targetCount) {
                    break
                }
                if (candidate.tag.isNullOrBlank()) {
                    compat.cancel(candidate.id)
                } else {
                    compat.cancel(candidate.tag, candidate.id)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channelId = candidate.notification.channelId
                    if (!channelId.isNullOrBlank() && channelId != SHORTCUT_CHANNEL_ID) {
                        try {
                            notificationManager.deleteNotificationChannel(channelId)
                        } catch (_: Throwable) {
                        }
                    }
                }
                currentCount -= 1
                Log.d(
                    TAG,
                    "trimActiveNotifications cancel id=${candidate.id} tag=${candidate.tag} reason=$reason currentCount=$currentCount targetCount=$targetCount",
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "trimActiveNotificationsIfNeeded failed reason=$reason error=${e.message}")
        }
    }

    private fun resolveNamedResourceId(
        context: Context,
        name: String,
    ): Int? {
        if (name.isBlank()) {
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

    private fun resolveId(
        context: Context,
        name: String,
    ): Int? {
        if (name.isBlank()) {
            return null
        }
        val id = context.resources.getIdentifier(name, "id", context.packageName)
        return id.takeIf { it != 0 }
    }

    private fun resolveSmallIcon(context: Context): Int {
        val icon = context.applicationInfo.icon
        return if (icon != 0) icon else android.R.drawable.ic_dialog_info
    }

    private fun createNotificationClickIntent(context: Context): Intent {
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(context, NotificationClickActivity::class.java)
        return launchIntent.apply {
            action = ACTION_NOTIFICATION_CLICK
            putExtra(EXTRA_FROM_NOTIFICATION_CLICK, true)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION,
            )
        }
    }

    private fun createShortcutClickPendingIntent(
        context: Context,
        requestCode: Int,
        payload: String,
        title: String,
    ): PendingIntent? {
        val clickIntent =
            createNotificationClickIntent(context).apply {
                putExtra(EXTRA_CLICK_EVENT, true)
                putExtra(EXTRA_ID, SHORTCUT_NOTIFICATION_ID)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_BODY, "")
                putExtra(EXTRA_PAYLOAD, payload)
            }
        return PendingIntent.getActivity(
            context,
            requestCode,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
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

    private fun resolveKeepAliveDebugDisplayTitle(
        context: Context,
        title: String?,
        payload: String?,
        source: String,
    ): String? {
        val baseTitle = resolveDebugDisplayTitle(context, title, payload)
        val isDebugBuild =
            context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (!isDebugBuild) {
            return baseTitle
        }
        val sourceLabel =
            when (source) {
                "work_manager" -> "WorkManager"
                "job_patrol" -> "JobScheduler"
                else -> ""
            }
        if (sourceLabel.isEmpty()) {
            return baseTitle
        }
        return if (baseTitle.isNullOrBlank()) {
            "[$sourceLabel]"
        } else {
            "$baseTitle [$sourceLabel]"
        }
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
}
