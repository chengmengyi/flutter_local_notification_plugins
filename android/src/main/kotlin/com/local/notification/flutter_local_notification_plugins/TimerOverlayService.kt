package com.local.notification.flutter_local_notification_plugins

import android.animation.AnimatorSet
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import kotlin.math.abs

class TimerOverlayService : Service() {
    companion object {
        private const val TAG = "TimerOverlayService"
        private const val ACTION_SHOW = "timer_overlay_action_show"
        private const val ACTION_CLOSE = "timer_overlay_action_close"
        private const val EXTRA_LAYOUT_NAME = "timer_overlay_layout_name"
        private const val EXTRA_USE_SECOND_LAYOUT = "timer_overlay_use_second_layout"
        private const val EXTRA_IS_BANNER = "timer_overlay_is_banner"
        private const val EXTRA_CLICK_TYPE = "timer_overlay_click_type"
        private const val EXTRA_BANNER_TITLES = "timer_overlay_banner_titles"
        private const val EXTRA_BANNER_ICONS = "timer_overlay_banner_icons"
        private const val EXTRA_BANNER_BUTTONS = "timer_overlay_banner_buttons"
        private const val EXTRA_BANNER_BUTTONS_2 = "timer_overlay_banner_buttons_2"
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
        private const val GRAVITY_TOP_CENTER_HORIZONTAL = 49

        @Volatile
        private var isShowing: Boolean = false

        fun show(
            context: Context,
            layoutName: String,
            useSecondLayout: Boolean,
            isBanner: Boolean,
            clickType: String,
            bannerContents: List<TimerOverlayHelper.TimerOverlayContent>,
            title: String,
            desc: String,
            button: String,
            button2: String?,
            useLastPdfInfo: Boolean,
            continueReadingStr: String?,
        ) {
            if (FlutterLocalNotificationPluginsPlugin.isDocumentScannerVisible(context)) {
                Log.d(TAG, "show skipped, document scanner visible")
                return
            }
            if (!TimerOverlayHelper.canDrawOverlaysByReflection(context)) {
                Log.d(TAG, "show skipped, overlay permission missing")
                return
            }
            val intent =
                Intent(context, TimerOverlayService::class.java).apply {
                    action = ACTION_SHOW
                    putExtra(EXTRA_LAYOUT_NAME, layoutName)
                    putExtra(EXTRA_USE_SECOND_LAYOUT, useSecondLayout)
                    putExtra(EXTRA_IS_BANNER, isBanner)
                    putExtra(EXTRA_CLICK_TYPE, clickType)
                    putStringArrayListExtra(EXTRA_BANNER_TITLES, ArrayList(bannerContents.map { it.title }))
                    putStringArrayListExtra(EXTRA_BANNER_ICONS, ArrayList(bannerContents.map { it.desc }))
                    putStringArrayListExtra(EXTRA_BANNER_BUTTONS, ArrayList(bannerContents.map { it.button }))
                    putStringArrayListExtra(EXTRA_BANNER_BUTTONS_2, ArrayList(bannerContents.map { it.button2.orEmpty() }))
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
    private var bannerDisplayContents: List<TimerOverlayHelper.TimerOverlayDisplayContent> = emptyList()
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

    private fun readBannerContents(intent: Intent?): List<TimerOverlayHelper.TimerOverlayContent> {
        val titles = intent?.getStringArrayListExtra(EXTRA_BANNER_TITLES).orEmpty()
        val icons = intent?.getStringArrayListExtra(EXTRA_BANNER_ICONS).orEmpty()
        val buttons = intent?.getStringArrayListExtra(EXTRA_BANNER_BUTTONS).orEmpty()
        val buttons2 = intent?.getStringArrayListExtra(EXTRA_BANNER_BUTTONS_2).orEmpty()
        return titles.indices.take(4).map { index ->
            TimerOverlayHelper.TimerOverlayContent(
                title = titles[index],
                desc = icons.getOrNull(index).orEmpty(),
                button = buttons.getOrNull(index).orEmpty(),
                button2 = buttons2.getOrNull(index).orEmpty(),
            )
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
            if (FlutterLocalNotificationPluginsPlugin.isDocumentScannerVisible(applicationContext)) {
                Log.d(TAG, "onStartCommand skipped, document scanner visible")
                stopSelf()
                return START_NOT_STICKY
            }
            if (!TimerOverlayHelper.canDrawOverlaysByReflection(applicationContext)) {
                stopSelf()
                return START_NOT_STICKY
            }
            val layoutName = intent?.getStringExtra(EXTRA_LAYOUT_NAME).orEmpty()
            val isBanner = intent?.getBooleanExtra(EXTRA_IS_BANNER, false) == true
            if (layoutName.isBlank() && !isBanner) {
                stopSelf()
                return START_NOT_STICKY
            }
            showOverlay(
                layoutName = layoutName,
                useSecondLayout = intent?.getBooleanExtra(EXTRA_USE_SECOND_LAYOUT, false) == true,
                isBanner = isBanner,
                clickType = intent?.getStringExtra(EXTRA_CLICK_TYPE).orEmpty(),
                bannerContents = readBannerContents(intent),
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
        useSecondLayout: Boolean,
        isBanner: Boolean,
        clickType: String,
        bannerContents: List<TimerOverlayHelper.TimerOverlayContent>,
        title: String,
        desc: String,
        button: String,
        button2: String?,
        useLastPdfInfo: Boolean,
        continueReadingStr: String?,
    ) {
        if (FlutterLocalNotificationPluginsPlugin.isDocumentScannerVisible(applicationContext)) {
            Log.d(TAG, "showOverlay skipped, document scanner visible")
            stopSelf()
            return
        }
        removeOverlay()
        val layoutResId = if (isBanner) {
            R.layout.fln_timer_top_banner
        } else {
            resources.getIdentifier(layoutName, "layout", packageName)
        }
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
                        val clickEvent = TimerOverlayHelper.cacheClickEvent(
                            context = applicationContext,
                            layoutName = layoutName,
                            clickType = clickType,
                            content = pendingDisplayContent,
                        )
                        val started = FlutterLocalNotificationPluginsPlugin.startTimerOverlayClickIntent(
                            context = applicationContext,
                            event = clickEvent,
                        )
                        stopSelf()
                        if (!started) {
                            TimerOverlayHelper.dispatchClickEvent(
                                context = applicationContext,
                                event = clickEvent,
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "overlay click failed", e)
                    }
                }
            }
        pendingDisplayContent = if (isBanner) {
            bindBannerContents(view, bannerContents).also {
                bannerDisplayContents = it
            }.firstOrNull() ?: TimerOverlayHelper.TimerOverlayDisplayContent(title, desc, button, button2)
        } else {
            bindContent(view, title, desc, button, button2, useLastPdfInfo, continueReadingStr)
        }
        if (isBanner) {
            bindBannerGestures(view)
        } else {
            bindCloseAction(view, useSecondLayout)
        }
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
                    gravity = if (isBanner) GRAVITY_TOP_CENTER_HORIZONTAL else GRAVITY_CENTER,
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
            if (isBanner) startBannerEnterAnimation(view)
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

    private fun bindBannerContents(
        view: View,
        contents: List<TimerOverlayHelper.TimerOverlayContent>,
    ): List<TimerOverlayHelper.TimerOverlayDisplayContent> {
        view.findViewById<ImageView>(R.id.fln_timer_banner_logo)
            ?.setImageDrawable(applicationInfo.loadIcon(packageManager))
        val iconIds = intArrayOf(
            R.id.fln_timer_banner_icon_1,
            R.id.fln_timer_banner_icon_2,
            R.id.fln_timer_banner_icon_3,
            R.id.fln_timer_banner_icon_4,
        )
        val textIds = intArrayOf(
            R.id.fln_timer_banner_text_1,
            R.id.fln_timer_banner_text_2,
            R.id.fln_timer_banner_text_3,
            R.id.fln_timer_banner_text_4,
        )
        return contents.take(4).mapIndexed { index, content ->
            view.findViewById<TextView>(textIds[index])?.text = content.title
            view.findViewById<ImageView>(iconIds[index])?.let { iconView ->
                val drawableId = resources.getIdentifier(content.desc, "drawable", packageName)
                val mipmapId = resources.getIdentifier(content.desc, "mipmap", packageName)
                val iconResId = drawableId.takeIf { it != 0 } ?: mipmapId.takeIf { it != 0 }
                if (iconResId != null) {
                    iconView.setImageResource(iconResId)
                } else {
                    iconView.setImageDrawable(applicationInfo.loadIcon(packageManager))
                    Log.d(TAG, "banner icon missing name=${content.desc}, fallback app icon")
                }
            }
            TimerOverlayHelper.TimerOverlayDisplayContent(
                title = content.title,
                desc = content.desc,
                button = content.button,
                button2 = content.button2,
            )
        }
    }

    private fun bindBannerGestures(view: View) {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var dragging = false
        view.setOnTouchListener { target, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    target.animate().cancel()
                    downX = event.rawX
                    downY = event.rawY
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - downX
                    val deltaY = event.rawY - downY
                    if (!dragging && (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop)) {
                        dragging = true
                    }
                    if (dragging) {
                        target.translationX = deltaX.coerceAtLeast(0f)
                        target.translationY = deltaY.coerceAtMost(0f)
                        val progress = maxOf(
                            target.translationX / target.width.coerceAtLeast(1),
                            -target.translationY / target.height.coerceAtLeast(1),
                        ).coerceIn(0f, 1f)
                        target.alpha = 1f - progress * 0.65f
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        selectBannerContentAt(target, event.rawX.toInt(), event.rawY.toInt())
                        target.performClick()
                    } else {
                        val dismissRight = target.translationX >= target.width * 0.3f
                        val dismissUp = -target.translationY >= target.height * 0.3f
                        if (dismissRight || dismissUp) {
                            dismissBanner(target, dismissRight)
                        } else {
                            target.animate()
                                .translationX(0f)
                                .translationY(0f)
                                .alpha(1f)
                                .setDuration(180L)
                                .start()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    target.animate()
                        .translationX(0f)
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(180L)
                        .start()
                    true
                }
                else -> false
            }
        }
    }

    private fun selectBannerContentAt(view: View, rawX: Int, rawY: Int) {
        val actionIds = intArrayOf(
            R.id.fln_timer_banner_action_1,
            R.id.fln_timer_banner_action_2,
            R.id.fln_timer_banner_action_3,
            R.id.fln_timer_banner_action_4,
        )
        actionIds.forEachIndexed { index, id ->
            val actionView = view.findViewById<View>(id) ?: return@forEachIndexed
            val bounds = Rect()
            if (actionView.getGlobalVisibleRect(bounds) && bounds.contains(rawX, rawY)) {
                pendingDisplayContent = bannerDisplayContents.getOrNull(index)
                return
            }
        }
    }

    private fun startBannerEnterAnimation(view: View) {
        view.post {
            view.translationY = -view.height.toFloat() - resolveStatusBarHeight()
            view.alpha = 0.7f
            view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun dismissBanner(view: View, toRight: Boolean) {
        view.animate()
            .translationX(if (toRight) resources.displayMetrics.widthPixels.toFloat() else view.translationX)
            .translationY(if (toRight) view.translationY else -view.height.toFloat() - resolveStatusBarHeight())
            .alpha(0f)
            .setDuration(220L)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.INVISIBLE
                    view.animate().setListener(null)
                    removeOverlay()
                    stopSelf()
                }
            })
            .start()
    }

    private fun resolveStatusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id == 0) 0 else resources.getDimensionPixelSize(id)
    }

    private fun bindCloseAction(
        view: View,
        useSecondLayout: Boolean,
    ) {
        val closeViewName = if (useSecondLayout) "later_btn_text" else "close_img"
        val closeViewId = resources.getIdentifier(closeViewName, "id", packageName)
        if (closeViewId == 0) {
            Log.d(TAG, "bindCloseAction view id missing name=$closeViewName")
            return
        }
        val closeView = view.findViewById<View>(closeViewId)
        if (closeView == null) {
            Log.d(TAG, "bindCloseAction view missing name=$closeViewName")
            return
        }
        closeView.setOnClickListener {
            try {
                removeOverlay()
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "close overlay click failed name=$closeViewName", e)
            }
        }
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
        bannerDisplayContents = emptyList()
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
