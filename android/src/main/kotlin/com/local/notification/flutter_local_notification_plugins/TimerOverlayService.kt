package com.local.notification.flutter_local_notification_plugins

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide

class TimerOverlayService : Service() {
    companion object {
        private const val TAG = "TimerOverlayService"
        private const val ACTION_SHOW = "timer_overlay_action_show"
        private const val ACTION_CLOSE = "timer_overlay_action_close"
        private const val EXTRA_LAYOUT_NAME = "timer_overlay_layout_name"
        private const val EXTRA_TITLE = "timer_overlay_title"
        private const val EXTRA_DESC = "timer_overlay_desc"
        private const val EXTRA_BUTTON = "timer_overlay_button"
        private const val CHANNEL_ID = "timer_overlay_channel"
        private const val CHANNEL_NAME = "Timer Overlay"
        private const val NOTIFICATION_ID = 12007

        fun show(
            context: Context,
            layoutName: String,
            title: String,
            desc: String,
            button: String,
        ) {
            if (!ProcessingOverlayService.isPermissionGranted(context)) {
                Log.d(TAG, "show skipped, overlay permission missing")
                return
            }
            val intent =
                Intent(context, TimerOverlayService::class.java).apply {
                    action = ACTION_SHOW
                    putExtra(EXTRA_LAYOUT_NAME, layoutName)
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_DESC, desc)
                    putExtra(EXTRA_BUTTON, button)
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun close(context: Context) {
            context.stopService(
                Intent(context, TimerOverlayService::class.java).apply {
                    action = ACTION_CLOSE
                },
            )
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var pendingDisplayContent: TimerOverlayHelper.TimerOverlayDisplayContent? = null
    private var buttonPulseAnimator: AnimatorSet? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
        ensureForegroundNotification()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_CLOSE) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!ProcessingOverlayService.isPermissionGranted(applicationContext)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val layoutName = intent?.getStringExtra(EXTRA_LAYOUT_NAME).orEmpty()
        if (layoutName.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        showOverlay(
            layoutName = layoutName,
            title = intent?.getStringExtra(EXTRA_TITLE).orEmpty(),
            desc = intent?.getStringExtra(EXTRA_DESC).orEmpty(),
            button = intent?.getStringExtra(EXTRA_BUTTON).orEmpty(),
        )
        ensureForegroundNotification()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    private fun showOverlay(
        layoutName: String,
        title: String,
        desc: String,
        button: String,
    ) {
        removeOverlay()
        val layoutResId = resources.getIdentifier(layoutName, "layout", packageName)
        if (layoutResId == 0) {
            Log.d(TAG, "showOverlay layout missing layoutName=$layoutName")
            stopSelf()
            return
        }
        val view =
            LayoutInflater.from(this).inflate(layoutResId, null, false).apply {
                isClickable = true
                setOnClickListener {
                    TimerOverlayHelper.cacheAndDispatchClickEvent(
                        context = applicationContext,
                        layoutName = layoutName,
                        content = pendingDisplayContent,
                    )
                    FlutterLocalNotificationPluginsPlugin.bringHostAppToForegroundOrStart(
                        applicationContext,
                    )
                    stopSelf()
                }
            }
        pendingDisplayContent = bindContent(view, title, desc, button)
        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.CENTER
            }
        try {
            windowManager?.addView(view, params)
            overlayView = view
            val displayContent = pendingDisplayContent
            if (displayContent?.shouldClearLastPdfInfoAfterDisplay == true) {
                TimerOverlayHelper.clearLastPdfInfoAfterDisplay(applicationContext)
            }
        } catch (e: Exception) {
            pendingDisplayContent = null
            Log.d(TAG, "showOverlay failed error=${e.message}")
            stopSelf()
        }
    }

    private fun bindContent(
        view: View,
        title: String,
        desc: String,
        button: String,
    ): TimerOverlayHelper.TimerOverlayDisplayContent {
        val appName = applicationInfo.loadLabel(packageManager).toString()
        val displayContent = TimerOverlayHelper.resolveDisplayContent(
            context = applicationContext,
            title = title,
            desc = desc,
            button = button,
        )
        findTextView(view, "app_name_text")?.text = appName
        findTextView(view, "title_text")?.text = displayContent.title
        findTextView(view, "desc_text")?.text = displayContent.desc
        findTextView(view, "btn_text")?.let { buttonView ->
            buttonView.text = displayContent.button
            startButtonPulse(buttonView)
        }
        findImageView(view, "icon_logo")?.let { logoView ->
            try {
                Glide
                    .with(applicationContext)
                    .load(applicationInfo.icon)
                    .circleCrop()
                    .into(logoView)
            } catch (e: Exception) {
                Log.d(TAG, "bind logo failed error=${e.message}")
            }
        }
        return displayContent
    }

    private fun findTextView(
        view: View,
        name: String,
    ): TextView? {
        val id = resources.getIdentifier(name, "id", packageName)
        return if (id == 0) null else view.findViewById(id)
    }

    private fun findImageView(
        view: View,
        name: String,
    ): ImageView? {
        val id = resources.getIdentifier(name, "id", packageName)
        return if (id == 0) null else view.findViewById(id)
    }

    private fun removeOverlay() {
        cancelButtonPulse()
        val view = overlayView ?: return
        try {
            windowManager?.removeView(view)
        } catch (e: Exception) {
            Log.d(TAG, "removeOverlay failed error=${e.message}")
        }
        overlayView = null
        pendingDisplayContent = null
    }

    private fun startButtonPulse(buttonView: View) {
        cancelButtonPulse()
        val scaleX =
            ObjectAnimator.ofFloat(buttonView, View.SCALE_X, 1f, 0.92f).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
            }
        val scaleY =
            ObjectAnimator.ofFloat(buttonView, View.SCALE_Y, 1f, 0.92f).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
            }
        buttonPulseAnimator =
            AnimatorSet().apply {
                duration = 420L
                interpolator = AccelerateDecelerateInterpolator()
                playTogether(scaleX, scaleY)
                start()
            }
    }

    private fun cancelButtonPulse() {
        buttonPulseAnimator?.cancel()
        buttonPulseAnimator = null
    }

    private fun ensureForegroundNotification() {
        createNotificationChannel()
        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(resolveSmallIcon())
                .setContentTitle(applicationInfo.loadLabel(packageManager).toString())
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
        manager.createNotificationChannel(channel)
    }

    private fun resolveSmallIcon(): Int {
        return FlutterLocalNotificationPluginsPlugin.resolveNotificationSmallIcon(this)
    }
}
