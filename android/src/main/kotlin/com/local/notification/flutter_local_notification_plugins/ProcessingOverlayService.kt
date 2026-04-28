package com.local.notification.flutter_local_notification_plugins

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.roundToInt

class ProcessingOverlayService : Service() {
    companion object {
        private const val TAG = "ProcessingOverlaySvc"
        private const val PREFS_NAME = "flutter_local_notification_plugins"
        private const val KEY_TASK_ID = "processing_overlay_task_id"
        private const val KEY_TITLE = "processing_overlay_title"
        private const val KEY_PROGRESS = "processing_overlay_progress"
        private const val KEY_LAUNCH_TASK_ID = "processing_overlay_launch_task_id"
        private const val NOTIFICATION_CHANNEL_ID = "pdf_flow_processing_overlay_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "PDF Flow Processing Overlay"
        private const val NOTIFICATION_ID = 10006

        const val ACTION_SHOW = "processing_overlay_action_show"
        const val ACTION_UPDATE = "processing_overlay_action_update"
        const val ACTION_CLOSE = "processing_overlay_action_close"
        const val EXTRA_CLICK_EVENT = "processing_overlay_click_event"
        const val EXTRA_TASK_ID = "processing_overlay_task_id"
        const val EXTRA_TITLE = "processing_overlay_title"
        const val EXTRA_PROGRESS = "processing_overlay_progress"
        private const val ACTION_NOTIFICATION_CLICK =
            "com.local.notification.flutter_local_notification_plugins.PROCESSING_OVERLAY_CLICK"

        @Volatile
        var isRunning: Boolean = false

        fun isPermissionGranted(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        fun show(
            context: Context,
            taskId: String,
            title: String,
            progress: Double,
        ) {
            if (!isPermissionGranted(context)) {
                Log.d(TAG, "show skipped, overlay permission missing")
                return
            }
            startService(
                context,
                Intent(context, ProcessingOverlayService::class.java).apply {
                    action = ACTION_SHOW
                    putExtra(EXTRA_TASK_ID, taskId)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_PROGRESS, progress)
                },
            )
        }

        fun update(
            context: Context,
            taskId: String,
            title: String,
            progress: Double,
        ) {
            if (!isPermissionGranted(context)) {
                Log.d(TAG, "update skipped, overlay permission missing")
                return
            }
            startService(
                context,
                Intent(context, ProcessingOverlayService::class.java).apply {
                    action = ACTION_UPDATE
                    putExtra(EXTRA_TASK_ID, taskId)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_PROGRESS, progress)
                },
            )
        }

        fun close(context: Context) {
            val intent =
                Intent(context, ProcessingOverlayService::class.java).apply {
                    action = ACTION_CLOSE
                }
            context.stopService(intent)
        }

        fun consumeLaunchTaskId(context: Context): String? {
            val taskId = prefs(context).getString(KEY_LAUNCH_TASK_ID, null)
            prefs(context).edit().remove(KEY_LAUNCH_TASK_ID).apply()
            return taskId
        }

        fun cacheLaunchTaskId(
            context: Context,
            taskId: String,
        ) {
            prefs(context).edit().putString(KEY_LAUNCH_TASK_ID, taskId).apply()
        }

        private fun startService(
            context: Context,
            intent: Intent,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        private fun prefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayLayoutParams: WindowManager.LayoutParams? = null
    private var logoView: ImageView? = null
    private var progressRingView: ProcessingOverlayProgressRingView? = null
    private var currentTaskId: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
        ensureForegroundNotification(
            title = getStringResource(R.string.fln_processing_overlay_default_title),
            progressPercent = 0,
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val action = intent?.action ?: ACTION_SHOW
        if (action == ACTION_CLOSE) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!isPermissionGranted(applicationContext)) {
            Log.d(TAG, "onStartCommand stop, overlay permission missing")
            stopSelf()
            return START_NOT_STICKY
        }
        val state = extractState(intent) ?: readStoredState()
        if (state == null || state.taskId.isBlank()) {
            Log.d(TAG, "onStartCommand stop, empty state")
            stopSelf()
            return START_NOT_STICKY
        }
        saveState(state)
        currentTaskId = state.taskId
        ensureOverlayView()
        updateOverlayView(state)
        ensureForegroundNotification(
            title = state.title,
            progressPercent = state.progressPercent,
        )
        isRunning = true
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlayView()
        clearStoredState()
        isRunning = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    private fun ensureOverlayView() {
        if (overlayView != null) {
            return
        }
        val rootView =
            LayoutInflater.from(this).inflate(
                R.layout.fln_processing_overlay_view,
                FrameLayout(this),
                false,
            )
        logoView = rootView.findViewById(R.id.fln_processing_logo)
        progressRingView = rootView.findViewById(R.id.fln_processing_overlay_progress_ring)
        applyOverlaySizes(rootView)
        bindAppLogo()
        val overlayHeight = resolveOverlayHeight(rootView)
        val params =
            WindowManager.LayoutParams(
                resolveOverlayWidthPx(),
                overlayHeight,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = dpToPx(10)
                y = resolveOverlayMinY()
            }
        attachOverlayInteractions(rootView, params)
        try {
            windowManager?.addView(rootView, params)
            overlayView = rootView
            overlayLayoutParams = params
        } catch (e: Exception) {
            Log.d(TAG, "ensureOverlayView failed error=${e.message}")
        }
    }

    private fun applyOverlaySizes(rootView: View) {
        val overlaySize = screenWToPx(68)
        val iconSize = screenWToPx(56)
        rootView.layoutParams = ViewGroup.LayoutParams(overlaySize, overlaySize)
        progressRingView?.layoutParams =
            FrameLayout.LayoutParams(overlaySize, overlaySize, Gravity.CENTER)
        logoView?.layoutParams =
            FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
    }

    private fun resolveOverlayHeight(rootView: View): Int {
        val height = rootView.layoutParams?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT
        return if (height > 0) {
            height
        } else {
            WindowManager.LayoutParams.WRAP_CONTENT
        }
    }

    private fun bindAppLogo() {
        val targetView = logoView ?: return
        try {
            targetView.setImageDrawable(packageManager.getApplicationIcon(packageName))
        } catch (e: Exception) {
            Log.d(TAG, "bindAppLogo failed error=${e.message}")
            targetView.setImageResource(resolveSmallIcon())
        }
        targetView.clipToOutline = true
    }

    private fun attachOverlayInteractions(
        rootView: View,
        params: WindowManager.LayoutParams,
    ) {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        rootView.isClickable = true
        rootView.setOnClickListener {
            handleOverlayClick()
        }
        rootView.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - downRawX).toInt()
                    val deltaY = (event.rawY - downRawY).toInt()
                    if (!dragging && (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop)) {
                        dragging = true
                    }
                    if (dragging) {
                        params.x = (startX + deltaX).coerceIn(0, resolveMaxOverlayX(view))
                        params.y =
                            (startY + deltaY).coerceIn(
                                resolveOverlayMinY(),
                                resolveMaxOverlayY(view),
                            )
                        try {
                            overlayLayoutParams = params
                            windowManager?.updateViewLayout(view, params)
                        } catch (e: Exception) {
                            Log.d(TAG, "updateOverlayDrag failed error=${e.message}")
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        view.performClick()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    private fun resolveOverlayWidthPx(): Int {
        return screenWToPx(68)
    }

    private fun resolveOverlayMinY(): Int {
        return resolveStatusBarHeightPx() + dpToPx(10)
    }

    private fun resolveMaxOverlayX(view: View): Int {
        return (resources.displayMetrics.widthPixels - view.width).coerceAtLeast(0)
    }

    private fun resolveMaxOverlayY(view: View): Int {
        return (resources.displayMetrics.heightPixels - view.height).coerceAtLeast(resolveOverlayMinY())
    }

    private fun resolveStatusBarHeightPx(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId <= 0) {
            return 0
        }
        return resources.getDimensionPixelSize(resourceId)
    }

    private fun dpToPx(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    private fun screenWToPx(value: Int): Int {
        return (value * resources.displayMetrics.widthPixels / 375f).roundToInt()
    }

    private fun updateOverlayView(state: OverlayState) {
        progressRingView?.progress = state.progressPercent
    }

    private fun removeOverlayView() {
        val view = overlayView ?: return
        try {
            windowManager?.removeView(view)
        } catch (e: Exception) {
            Log.d(TAG, "removeOverlayView failed error=${e.message}")
        }
        overlayView = null
        overlayLayoutParams = null
        logoView = null
        progressRingView = null
    }

    private fun handleOverlayClick() {
        val taskId = currentTaskId.ifBlank { readStoredState()?.taskId ?: "" }
        if (taskId.isBlank()) {
            return
        }
        cacheLaunchTaskId(applicationContext, taskId)
        val launchIntent =
            createLaunchIntent()?.apply {
                putExtra(EXTRA_CLICK_EVENT, true)
                putExtra(EXTRA_TASK_ID, taskId)
            }
        if (launchIntent == null) {
            Log.d(TAG, "handleOverlayClick launch intent missing")
            return
        }
        try {
            startActivity(launchIntent)
        } catch (e: Exception) {
            Log.d(TAG, "handleOverlayClick failed error=${e.message}")
        }
    }

    private fun ensureForegroundNotification(
        title: String,
        progressPercent: Int,
    ) {
        createNotificationChannel()
        val launchIntent = createLaunchIntent()
        val pendingIntent =
            launchIntent?.let {
                PendingIntent.getActivity(
                    this,
                    NOTIFICATION_ID,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
        val builder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(resolveSmallIcon())
                .setContentTitle(title.ifBlank { getStringResource(R.string.fln_processing_overlay_default_title) })
                .setContentText("$progressPercent%")
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        startForeground(NOTIFICATION_ID, builder.build())
    }

    private fun createLaunchIntent(): Intent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        val component = launchIntent.component ?: return launchIntent.apply {
            action = ACTION_NOTIFICATION_CLICK
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
            )
        }
        return Intent().apply {
            setComponent(component)
            setPackage(packageName)
            action = ACTION_NOTIFICATION_CLICK
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
            )
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) {
            return
        }
        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getStringResource(R.string.fln_processing_overlay_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
        manager.createNotificationChannel(channel)
    }

    private fun resolveSmallIcon(): Int {
        val icon = applicationInfo.icon
        return if (icon != 0) {
            icon
        } else {
            android.R.drawable.ic_dialog_info
        }
    }

    private fun extractState(intent: Intent?): OverlayState? {
        intent ?: return null
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return null
        val title =
            intent.getStringExtra(EXTRA_TITLE)
                ?: getStringResource(R.string.fln_processing_overlay_default_title)
        val progress = intent.getDoubleExtra(EXTRA_PROGRESS, 0.0)
        return OverlayState(
            taskId = taskId,
            title = title,
            progress = progress.coerceIn(0.0, 1.0),
        )
    }

    private fun saveState(state: OverlayState) {
        prefs().edit()
            .putString(KEY_TASK_ID, state.taskId)
            .putString(KEY_TITLE, state.title)
            .putFloat(KEY_PROGRESS, state.progress.toFloat())
            .apply()
    }

    private fun readStoredState(): OverlayState? {
        val sharedPrefs = prefs()
        val taskId = sharedPrefs.getString(KEY_TASK_ID, null) ?: return null
        val title =
            sharedPrefs.getString(
                KEY_TITLE,
                getStringResource(R.string.fln_processing_overlay_default_title),
            ) ?: getStringResource(R.string.fln_processing_overlay_default_title)
        val progress = sharedPrefs.getFloat(KEY_PROGRESS, 0f).toDouble()
        return OverlayState(taskId = taskId, title = title, progress = progress)
    }

    private fun clearStoredState() {
        prefs().edit()
            .remove(KEY_TASK_ID)
            .remove(KEY_TITLE)
            .remove(KEY_PROGRESS)
            .apply()
    }

    private fun prefs(): SharedPreferences {
        return applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getStringResource(resId: Int): String {
        return try {
            getString(resId)
        } catch (_: Exception) {
            "AI Processing..."
        }
    }

    private data class OverlayState(
        val taskId: String,
        val title: String,
        val progress: Double,
    ) {
        val progressPercent: Int
            get() = (progress.coerceIn(0.0, 1.0) * 100).roundToInt()
    }
}
