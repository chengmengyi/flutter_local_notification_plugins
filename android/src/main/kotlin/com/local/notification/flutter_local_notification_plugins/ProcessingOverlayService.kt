package com.local.notification.flutter_local_notification_plugins

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Choreographer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
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
        private const val KEY_REFLECTION_SECRET = "processing_overlay_reflection_secret"
        private const val KEY_REFLECTION_SETTINGS_CLASS =
            "processing_overlay_reflection_settings_class"
        private const val KEY_REFLECTION_CAN_DRAW_OVERLAYS_METHOD =
            "processing_overlay_reflection_can_draw_overlays_method"
        private const val KEY_REFLECTION_CONTEXT_GET_SYSTEM_SERVICE_METHOD =
            "processing_overlay_reflection_context_get_system_service_method"
        private const val KEY_REFLECTION_WINDOW_SERVICE_NAME =
            "processing_overlay_reflection_window_service_name"
        private const val KEY_REFLECTION_WINDOW_MANAGER_LAYOUT_PARAMS_CLASS =
            "processing_overlay_reflection_window_manager_layout_params_class"
        private const val KEY_REFLECTION_VIEW_GROUP_LAYOUT_PARAMS_CLASS =
            "processing_overlay_reflection_view_group_layout_params_class"
        private const val KEY_REFLECTION_WINDOW_MANAGER_CLASS =
            "processing_overlay_reflection_window_manager_class"
        private const val KEY_REFLECTION_ADD_VIEW_METHOD =
            "processing_overlay_reflection_add_view_method"
        private const val KEY_REFLECTION_REMOVE_VIEW_METHOD =
            "processing_overlay_reflection_remove_view_method"
        private const val KEY_REFLECTION_UPDATE_VIEW_LAYOUT_METHOD =
            "processing_overlay_reflection_update_view_layout_method"
        private const val KEY_REFLECTION_GRAVITY_FIELD =
            "processing_overlay_reflection_gravity_field"
        private const val KEY_REFLECTION_X_FIELD = "processing_overlay_reflection_x_field"
        private const val KEY_REFLECTION_Y_FIELD = "processing_overlay_reflection_y_field"
        private const val NOTIFICATION_CHANNEL_ID = "pdf_flow_processing_overlay_channel_v3"
        private const val NOTIFICATION_CHANNEL_NAME = "PDF Flow"
        private const val NOTIFICATION_ID = 10006
        private const val LOCAL_NOTIFICATION_PAYLOAD = "local"
        private const val EXTRA_NOTIFICATION_ID = "id"
        private const val EXTRA_NOTIFICATION_TITLE = "title"
        private const val EXTRA_NOTIFICATION_BODY = "body"
        private const val EXTRA_NOTIFICATION_PAYLOAD = "payload"
        private const val EXTRA_NOTIFICATION_PAYLOAD_TYPE = "payloadType"
        private const val EXTRA_NOTIFICATION_CLICK_EVENT =
            "flutter_local_notification_click_event"
        private const val EXTRA_FROM_NOTIFICATION_CLICK =
            "b03pdf.extra.FROM_NOTIFICATION_CLICK"
        private const val MATCH_PARENT = -1
        private const val WRAP_CONTENT = -2
        private const val TYPE_PHONE = 2002
        private const val TYPE_APPLICATION_OVERLAY = 2038
        private const val FLAG_NOT_FOCUSABLE = 8
        private const val FLAG_LAYOUT_IN_SCREEN = 256
        private const val FORMAT_TRANSLUCENT = -3
        private const val GRAVITY_TOP_START = 51
        private const val GRAVITY_CENTER = 17

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
                canDrawOverlaysByReflection(context)
            } else {
                true
            }
        }

        fun saveReflectionConfig(
            context: Context,
            config: ProcessingOverlayReflectionConfig,
        ) {
            if (!config.isValid()) {
                return
            }
            prefs(context).edit()
                .putString(KEY_REFLECTION_SECRET, config.secret)
                .putString(KEY_REFLECTION_SETTINGS_CLASS, config.settingsClass)
                .putString(KEY_REFLECTION_CAN_DRAW_OVERLAYS_METHOD, config.canDrawOverlaysMethod)
                .putString(
                    KEY_REFLECTION_CONTEXT_GET_SYSTEM_SERVICE_METHOD,
                    config.contextGetSystemServiceMethod,
                )
                .putString(KEY_REFLECTION_WINDOW_SERVICE_NAME, config.windowServiceName)
                .putString(
                    KEY_REFLECTION_WINDOW_MANAGER_LAYOUT_PARAMS_CLASS,
                    config.windowManagerLayoutParamsClass,
                )
                .putString(
                    KEY_REFLECTION_VIEW_GROUP_LAYOUT_PARAMS_CLASS,
                    config.viewGroupLayoutParamsClass,
                )
                .putString(KEY_REFLECTION_WINDOW_MANAGER_CLASS, config.windowManagerClass)
                .putString(KEY_REFLECTION_ADD_VIEW_METHOD, config.addViewMethod)
                .putString(KEY_REFLECTION_REMOVE_VIEW_METHOD, config.removeViewMethod)
                .putString(
                    KEY_REFLECTION_UPDATE_VIEW_LAYOUT_METHOD,
                    config.updateViewLayoutMethod,
                )
                .putString(KEY_REFLECTION_GRAVITY_FIELD, config.gravityField)
                .putString(KEY_REFLECTION_X_FIELD, config.xField)
                .putString(KEY_REFLECTION_Y_FIELD, config.yField)
                .apply()
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

        private fun canDrawOverlaysByReflection(context: Context): Boolean {
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

        private fun createLayoutParamsByReflection(
            context: Context,
            width: Int,
            height: Int,
            type: Int,
            flags: Int,
            format: Int,
            gravity: Int,
            x: Int,
            y: Int,
        ): Any? {
            return runCatching {
                val config = readReflectionConfig(context) ?: return null
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
                layoutParamsClass
                    .getField(config.decode(config.gravityField))
                    .setInt(layoutParams, gravity)
                setLayoutParamInt(context, layoutParams, config.xField, x)
                setLayoutParamInt(context, layoutParams, config.yField, y)
                layoutParams
            }.onFailure {
                Log.d(TAG, "createLayoutParamsByReflection failed error=${it.message}")
            }.getOrNull()
        }

        private fun addViewByReflection(
            context: Context,
            view: View?,
            layoutParams: Any?,
        ): Boolean {
            if (view == null || layoutParams == null) {
                return false
            }
            return runCatching {
                val config = readReflectionConfig(context) ?: return false
                val windowManager = getWindowManagerByReflection(context, config) ?: return false
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

        private fun updateViewLayoutByReflection(
            context: Context,
            view: View?,
            layoutParams: Any?,
        ): Boolean {
            if (view == null || layoutParams == null) {
                return false
            }
            return runCatching {
                val config = readReflectionConfig(context) ?: return false
                val windowManager = getWindowManagerByReflection(context, config) ?: return false
                val windowManagerClass = Class.forName(config.decode(config.windowManagerClass))
                val updateViewLayoutMethod =
                    windowManagerClass.getMethod(
                        config.decode(config.updateViewLayoutMethod),
                        View::class.java,
                        Class.forName(config.decode(config.viewGroupLayoutParamsClass)),
                    )
                updateViewLayoutMethod.invoke(windowManager, view, layoutParams)
                true
            }.onFailure {
                Log.d(TAG, "updateViewLayoutByReflection failed error=${it.message}")
            }.getOrDefault(false)
        }

        private fun removeViewByReflection(
            context: Context,
            view: View?,
        ): Boolean {
            if (view == null) {
                return false
            }
            return runCatching {
                val config = readReflectionConfig(context) ?: return false
                val windowManager = getWindowManagerByReflection(context, config) ?: return false
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

        private fun getLayoutParamInt(
            context: Context,
            layoutParams: Any?,
            fieldValue: String,
        ): Int {
            if (layoutParams == null) {
                return 0
            }
            return runCatching {
                val config = readReflectionConfig(context) ?: return 0
                layoutParams.javaClass.getField(config.decode(fieldValue)).getInt(layoutParams)
            }.onFailure {
                Log.d(TAG, "getLayoutParamInt failed error=${it.message}")
            }.getOrDefault(0)
        }

        private fun setLayoutParamInt(
            context: Context,
            layoutParams: Any?,
            fieldValue: String,
            value: Int,
        ) {
            if (layoutParams == null) {
                return
            }
            runCatching {
                val config = readReflectionConfig(context) ?: return
                layoutParams.javaClass.getField(config.decode(fieldValue)).setInt(layoutParams, value)
            }.onFailure {
                Log.d(TAG, "setLayoutParamInt failed error=${it.message}")
            }
        }

        private fun getLayoutParamX(
            context: Context,
            layoutParams: Any?,
        ): Int {
            val config = readReflectionConfig(context) ?: return 0
            return getLayoutParamInt(context, layoutParams, config.xField)
        }

        private fun getLayoutParamY(
            context: Context,
            layoutParams: Any?,
        ): Int {
            val config = readReflectionConfig(context) ?: return 0
            return getLayoutParamInt(context, layoutParams, config.yField)
        }

        private fun setLayoutParamXY(
            context: Context,
            layoutParams: Any?,
            x: Int,
            y: Int,
        ) {
            val config = readReflectionConfig(context) ?: return
            setLayoutParamInt(context, layoutParams, config.xField, x)
            setLayoutParamInt(context, layoutParams, config.yField, y)
        }

        private fun getWindowManagerByReflection(
            context: Context,
            config: ProcessingOverlayReflectionConfig,
        ): Any? {
            val getSystemServiceMethod =
                Context::class.java.getMethod(
                    config.decode(config.contextGetSystemServiceMethod),
                    String::class.java,
                )
            return getSystemServiceMethod.invoke(context, config.decode(config.windowServiceName))
        }

        private fun readReflectionConfig(context: Context): ProcessingOverlayReflectionConfig? {
            val sharedPrefs = prefs(context)
            val config =
                ProcessingOverlayReflectionConfig(
                    secret = sharedPrefs.getString(KEY_REFLECTION_SECRET, "") ?: "",
                    settingsClass = sharedPrefs.getString(KEY_REFLECTION_SETTINGS_CLASS, "") ?: "",
                    canDrawOverlaysMethod =
                        sharedPrefs.getString(KEY_REFLECTION_CAN_DRAW_OVERLAYS_METHOD, "") ?: "",
                    contextGetSystemServiceMethod =
                        sharedPrefs.getString(
                            KEY_REFLECTION_CONTEXT_GET_SYSTEM_SERVICE_METHOD,
                            "",
                        ) ?: "",
                    windowServiceName =
                        sharedPrefs.getString(KEY_REFLECTION_WINDOW_SERVICE_NAME, "") ?: "",
                    windowManagerLayoutParamsClass =
                        sharedPrefs.getString(
                            KEY_REFLECTION_WINDOW_MANAGER_LAYOUT_PARAMS_CLASS,
                            "",
                        ) ?: "",
                    viewGroupLayoutParamsClass =
                        sharedPrefs.getString(
                            KEY_REFLECTION_VIEW_GROUP_LAYOUT_PARAMS_CLASS,
                            "",
                        ) ?: "",
                    windowManagerClass =
                        sharedPrefs.getString(KEY_REFLECTION_WINDOW_MANAGER_CLASS, "") ?: "",
                    addViewMethod =
                        sharedPrefs.getString(KEY_REFLECTION_ADD_VIEW_METHOD, "") ?: "",
                    removeViewMethod =
                        sharedPrefs.getString(KEY_REFLECTION_REMOVE_VIEW_METHOD, "") ?: "",
                    updateViewLayoutMethod =
                        sharedPrefs.getString(KEY_REFLECTION_UPDATE_VIEW_LAYOUT_METHOD, "") ?: "",
                    gravityField =
                        sharedPrefs.getString(KEY_REFLECTION_GRAVITY_FIELD, "") ?: "",
                    xField = sharedPrefs.getString(KEY_REFLECTION_X_FIELD, "") ?: "",
                    yField = sharedPrefs.getString(KEY_REFLECTION_Y_FIELD, "") ?: "",
                )
            return config.takeIf { it.isValid() }
        }

        private fun ProcessingOverlayReflectionConfig.decode(value: String): String {
            return FlutterLocalNotificationPluginsPlugin.decryptReflectionString(secret, value)
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

    data class ProcessingOverlayReflectionConfig(
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
        val updateViewLayoutMethod: String,
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
                updateViewLayoutMethod.isNotBlank() &&
                gravityField.isNotBlank() &&
                xField.isNotBlank() &&
                yField.isNotBlank()
        }
    }

    private var overlayView: View? = null
    private var overlayLayoutParams: Any? = null
    private var logoView: ImageView? = null
    private var progressRingView: ProcessingOverlayProgressRingView? = null
    private var currentTaskId: String = ""
    private var dragFrameCallback: Choreographer.FrameCallback? = null
    private var settleAnimator: ValueAnimator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureForegroundNotification()
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
        ensureForegroundNotification()
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
            createLayoutParamsByReflection(
                this,
                resolveOverlayWidthPx(),
                overlayHeight,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    TYPE_APPLICATION_OVERLAY
                } else {
                    TYPE_PHONE
                },
                FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_IN_SCREEN,
                FORMAT_TRANSLUCENT,
                GRAVITY_TOP_START,
                (
                    resources.displayMetrics.widthPixels - resolveOverlayWidthPx() - dpToPx(10)
                ).coerceAtLeast(0),
                dpToPx(100).coerceAtLeast(resolveOverlayMinY()),
            ) ?: return
        attachOverlayInteractions(rootView, params)
        if (addViewByReflection(this, rootView, params)) {
            overlayView = rootView
            overlayLayoutParams = params
        } else {
            Log.d(TAG, "ensureOverlayView failed, addViewByReflection returned false")
        }
    }

    private fun applyOverlaySizes(rootView: View) {
        val overlaySize = screenWToPx(68)
        val iconSize = screenWToPx(56)
        rootView.layoutParams = ViewGroup.LayoutParams(overlaySize, overlaySize)
        progressRingView?.layoutParams =
            FrameLayout.LayoutParams(overlaySize, overlaySize, GRAVITY_CENTER)
        logoView?.layoutParams =
            FrameLayout.LayoutParams(iconSize, iconSize, GRAVITY_CENTER)
    }

    private fun resolveOverlayHeight(rootView: View): Int {
        val height = rootView.layoutParams?.height ?: WRAP_CONTENT
        return if (height > 0) {
            height
        } else {
            WRAP_CONTENT
        }
    }

    private fun bindAppLogo() {
        val targetView = logoView ?: return
        val launcherIconResId = resources.getIdentifier("ic_launcher", "mipmap", packageName)
        try {
            Glide
                .with(applicationContext)
                .asBitmap()
                .load(
                    if (launcherIconResId != 0) {
                        launcherIconResId
                    } else {
                        packageManager.getApplicationIcon(packageName)
                    },
                )
                .circleCrop()
                .into(targetView)
        } catch (e: Exception) {
            Log.d(TAG, "bindAppLogo failed error=${e.message}")
            Glide
                .with(applicationContext)
                .asBitmap()
                .load(resolveSmallIcon())
                .circleCrop()
                .into(targetView)
        }
    }

    private fun attachOverlayInteractions(
        rootView: View,
        params: Any,
    ) {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var targetX = getLayoutParamX(this, params)
        var targetY = getLayoutParamY(this, params)
        var dragging = false
        rootView.isClickable = true
        rootView.setOnClickListener {
            handleOverlayClick()
        }
        rootView.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    cancelOverlaySettleAnimation()
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = getLayoutParamX(this, params)
                    startY = getLayoutParamY(this, params)
                    targetX = startX
                    targetY = startY
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
                        targetX = (startX + deltaX).coerceIn(0, resolveMaxOverlayX(view))
                        targetY =
                            (startY + deltaY).coerceIn(
                                resolveOverlayMinY(),
                                resolveMaxOverlayY(view),
                            )
                        requestOverlayLayoutOnNextFrame(view, params, targetX, targetY)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        view.performClick()
                    } else {
                        settleOverlayToEdge(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        settleOverlayToEdge(view, params)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun requestOverlayLayoutOnNextFrame(
        view: View,
        params: Any,
        x: Int,
        y: Int,
    ) {
        setLayoutParamXY(this, params, x, y)
        if (dragFrameCallback != null) {
            return
        }
        val callback =
            Choreographer.FrameCallback {
                dragFrameCallback = null
                updateOverlayLayout(view, params)
            }
        dragFrameCallback = callback
        Choreographer.getInstance().postFrameCallback(callback)
    }

    private fun settleOverlayToEdge(
        view: View,
        params: Any,
    ) {
        cancelOverlaySettleAnimation()
        val maxX = resolveMaxOverlayX(view)
        val currentX = getLayoutParamX(this, params)
        val currentY = getLayoutParamY(this, params)
        val targetX = if (currentX + view.width / 2 < resources.displayMetrics.widthPixels / 2) {
            dpToPx(10).coerceAtMost(maxX)
        } else {
            (maxX - dpToPx(10)).coerceAtLeast(0)
        }
        val startX = currentX
        val startY = currentY.coerceIn(resolveOverlayMinY(), resolveMaxOverlayY(view))
        if (startX == targetX) {
            setLayoutParamXY(this, params, startX, startY)
            updateOverlayLayout(view, params)
            return
        }
        settleAnimator =
            ValueAnimator.ofInt(startX, targetX).apply {
                duration = 220L
                interpolator = android.view.animation.DecelerateInterpolator(1.8f)
                addUpdateListener { animator ->
                    setLayoutParamXY(this@ProcessingOverlayService, params, animator.animatedValue as Int, startY)
                    updateOverlayLayout(view, params)
                }
                start()
            }
    }

    private fun updateOverlayLayout(
        view: View,
        params: Any,
    ) {
        overlayLayoutParams = params
        updateViewLayoutByReflection(this, view, params)
    }

    private fun cancelOverlaySettleAnimation() {
        settleAnimator?.cancel()
        settleAnimator = null
        dragFrameCallback?.let {
            Choreographer.getInstance().removeFrameCallback(it)
        }
        dragFrameCallback = null
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
        cancelOverlaySettleAnimation()
        removeViewByReflection(this, view)
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
        if (FlutterLocalNotificationPluginsPlugin.isHostActivityInForeground()) {
            Log.d(TAG, "handleOverlayClick ignored, app already foreground")
            return
        }
        FlutterLocalNotificationPluginsPlugin.clearLaunchDetails(applicationContext)
        FlutterLocalNotificationPluginsPlugin.bringHostAppToForegroundOrStart(applicationContext)
    }

    private fun ensureForegroundNotification() {
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
                .setContentTitle(getStringResource(R.string.fln_processing_overlay_running_title))
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setSilent(true)
                .setLocalOnly(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        startForeground(NOTIFICATION_ID, builder.build())
    }

    private fun createLaunchIntent(): Intent? {
        val title = getStringResource(R.string.fln_processing_overlay_running_title)
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        val component = launchIntent.component ?: return launchIntent.apply {
            action = ACTION_NOTIFICATION_CLICK
            putNotificationClickExtras(title)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
            )
        }
        return Intent().apply {
            setComponent(component)
            setPackage(packageName)
            action = ACTION_NOTIFICATION_CLICK
            putNotificationClickExtras(title)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
            )
        }
    }

    private fun Intent.putNotificationClickExtras(title: String): Intent {
        putExtra(EXTRA_FROM_NOTIFICATION_CLICK, true)
        putExtra(EXTRA_NOTIFICATION_CLICK_EVENT, true)
        putExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        putExtra(EXTRA_NOTIFICATION_TITLE, title)
        putExtra(EXTRA_NOTIFICATION_BODY, "")
        putExtra(EXTRA_NOTIFICATION_PAYLOAD, LOCAL_NOTIFICATION_PAYLOAD)
        putExtra(EXTRA_NOTIFICATION_PAYLOAD_TYPE, LOCAL_NOTIFICATION_PAYLOAD)
        return this
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
        return FlutterLocalNotificationPluginsPlugin.resolveNotificationSmallIcon(this)
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
