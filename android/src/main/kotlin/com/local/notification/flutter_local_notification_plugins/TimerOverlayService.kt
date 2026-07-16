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
import android.graphics.Paint
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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
        private const val EXTRA_BUTTON_2 = "timer_overlay_button_2"
        private const val EXTRA_USE_LAST_PDF_INFO = "timer_overlay_use_last_pdf_info"
        private const val EXTRA_CONTINUE_READING_STR = "timer_overlay_continue_reading_str"
        private const val CHANNEL_ID = "timer_overlay_channel"
        private const val CHANNEL_NAME = "Timer Overlay"
        private const val NOTIFICATION_ID = 12007
        private const val LAYOUT_MATCH_PARENT = -1
        private const val LAYOUT_WRAP_CONTENT = -2
        private const val TYPE_APPLICATION_OVERLAY = 2038
        private const val TYPE_PHONE = 2002
        private const val FLAG_NOT_FOCUSABLE = 8
        private const val FORMAT_TRANSLUCENT = -3
        private const val GRAVITY_CENTER = 17

        @Volatile
        private var isShowing: Boolean = false

        fun show(
            context: Context,
            layoutName: String,
            title: String,
            desc: String,
            button: String,
            button2: String?,
            useLastPdfInfo: Boolean,
            continueReadingStr: String?,
        ) {
            if (!TimerOverlayHelper.canDrawOverlaysByReflection(context)) {
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
                    putExtra(EXTRA_BUTTON_2, button2)
                    putExtra(EXTRA_USE_LAST_PDF_INFO, useLastPdfInfo)
                    putExtra(EXTRA_CONTINUE_READING_STR, continueReadingStr)
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun close(context: Context) {
            if (!isShowing) {
                Log.d(TAG, "close skipped, no timer overlay showing")
                return
            }
            context.stopService(
                Intent(context, TimerOverlayService::class.java).apply {
                    action = ACTION_CLOSE
                },
            )
        }
    }

    private var overlayView: View? = null
    private var pendingDisplayContent: TimerOverlayHelper.TimerOverlayDisplayContent? = null
    private var buttonPulseAnimator: AnimatorSet? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            ensureForegroundNotification()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate failed", e)
            stopSelf()
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        try {
            if (intent?.action == ACTION_CLOSE) {
                stopSelf()
                return START_NOT_STICKY
            }
            if (!TimerOverlayHelper.canDrawOverlaysByReflection(applicationContext)) {
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
                button2 = intent?.getStringExtra(EXTRA_BUTTON_2),
                useLastPdfInfo = intent?.getBooleanExtra(EXTRA_USE_LAST_PDF_INFO, true) != false,
                continueReadingStr = intent?.getStringExtra(EXTRA_CONTINUE_READING_STR),
            )
            ensureForegroundNotification()
        } catch (e: Exception) {
            Log.e(TAG, "onStartCommand failed action=${intent?.action}", e)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try {
            removeOverlay()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy failed", e)
        } finally {
            super.onDestroy()
        }
    }

    private fun showOverlay(
        layoutName: String,
        title: String,
        desc: String,
        button: String,
        button2: String?,
        useLastPdfInfo: Boolean,
        continueReadingStr: String?,
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
                    try {
                        FlutterLocalNotificationPluginsPlugin.clearLaunchDetails(applicationContext)
                        TimerOverlayHelper.cacheAndDispatchClickEvent(
                            context = applicationContext,
                            layoutName = layoutName,
                            content = pendingDisplayContent,
                        )
                        FlutterLocalNotificationPluginsPlugin.bringHostAppToForegroundOrStart(
                            applicationContext,
                        )
                        stopSelf()
                    } catch (e: Exception) {
                        Log.e(TAG, "overlay click failed", e)
                    }
                }
            }
        pendingDisplayContent =
            bindContent(view, title, desc, button, button2, useLastPdfInfo, continueReadingStr)
        try {
            val added =
                TimerOverlayHelper.addViewByReflection(
                    context = applicationContext,
                    view = view,
                    width = LAYOUT_MATCH_PARENT,
                    height = LAYOUT_WRAP_CONTENT,
                    type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        TYPE_APPLICATION_OVERLAY
                    } else {
                        TYPE_PHONE
                    },
                    flags = FLAG_NOT_FOCUSABLE,
                    format = FORMAT_TRANSLUCENT,
                    gravity = GRAVITY_CENTER,
                    x = 0,
                    y = 0,
                )
            if (!added) {
                pendingDisplayContent = null
                Log.d(TAG, "showOverlay failed, addView reflection returned false")
                stopSelf()
                return
            }
            overlayView = view
            isShowing = true
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
        button2: String?,
        useLastPdfInfo: Boolean,
        continueReadingStr: String?,
    ): TimerOverlayHelper.TimerOverlayDisplayContent {
        val appName = applicationInfo.loadLabel(packageManager).toString()
        val displayContent = TimerOverlayHelper.resolveDisplayContent(
            context = applicationContext,
            title = title,
            desc = desc,
            button = button,
            button2 = button2,
            useLastPdfInfo = useLastPdfInfo,
        )
        findTextView(view, "app_name_text")?.text = appName
        findTextView(view, "title_text")?.text = displayContent.title
        findTextView(view, "desc_text")?.text = displayContent.desc
        if (useLastPdfInfo) {
            findTextView(view, "timer_text")?.text = continueReadingStr.orEmpty()
        }
        findTextView(view, "btn_text")?.let { buttonView ->
            buttonView.text = displayContent.button
            startButtonPulse(buttonView)
        }
        findTextView(view, "later_btn_text")?.let { laterButtonView ->
            val laterText = displayContent.button2?.takeUnless { it.isBlank() }
            if (laterText == null) {
                laterButtonView.visibility = View.GONE
            } else {
                laterButtonView.visibility = View.VISIBLE
                laterButtonView.text = laterText
                laterButtonView.paintFlags =
                    laterButtonView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            }
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
            TimerOverlayHelper.removeViewByReflection(applicationContext, view)
        } catch (e: Exception) {
            Log.d(TAG, "removeOverlay failed error=${e.message}")
        }
        overlayView = null
        pendingDisplayContent = null
        isShowing = false
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
