package com.local.notification.flutter_local_notification_plugins

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView

class OverlayPermissionGuideActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var switchOn = false
    private var switchView: ImageView? = null
    private var rightDownIconView: View? = null
    private val switchRunnable =
        object : Runnable {
            override fun run() {
                try {
                    animateSwitchIcon()
                    handler.postDelayed(this, SWITCH_ANIMATION_INTERVAL_MILLIS)
                } catch (e: Exception) {
                    Log.e("LocalNotificationPlugin", "switch animation failed", e)
                }
            }
        }
    private val rightDownIconRunnable =
        object : Runnable {
            override fun run() {
                try {
                    animateRightDownIcon()
                    handler.postDelayed(this, RIGHT_DOWN_ANIMATION_INTERVAL_MILLIS)
                } catch (e: Exception) {
                    Log.e("LocalNotificationPlugin", "guide animation failed", e)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LocalNotificationPlugin", "OverlayPermissionGuideActivity onCreate")
        overridePendingTransition(0, 0)
        enableImmersiveStatusBar(window)
        setContentView(resolveLayoutId())
        bindAppInfo()
        switchView = findViewById(R.id.icon_switch)
        switchView?.setImageResource(R.drawable.switch_close)
        handler.postDelayed(switchRunnable, SWITCH_ANIMATION_INTERVAL_MILLIS)
        rightDownIconView = findOptionalViewByName("right_down_icon")
        rightDownIconView?.let {
            handler.postDelayed(rightDownIconRunnable, RIGHT_DOWN_ANIMATION_INTERVAL_MILLIS)
        }
        findViewById<View>(R.id.fln_overlay_permission_guide_root)?.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(switchRunnable)
        handler.removeCallbacks(rightDownIconRunnable)
        rightDownIconView?.animate()?.cancel()
        super.onDestroy()
    }

    private fun bindAppInfo() {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            findViewById<ImageView>(R.id.icon_logo)?.setImageDrawable(
                packageManager.getApplicationIcon(appInfo),
            )
            val appName = packageManager.getApplicationLabel(appInfo)
            findViewById<TextView>(R.id.app_name_text)?.text = appName
            findViewById<TextView>(R.id.guide_title_text)?.text =
                intent.getStringExtra(
                    FlutterLocalNotificationPluginsPlugin.EXTRA_OVERLAY_PERMISSION_GUIDE_TITLE,
                )?.takeUnless { it.isBlank() }
                    ?.replaceAppNamePlaceholder(appName)
                    ?: getString(R.string.fln_overlay_permission_guide_title)
            findViewById<TextView>(R.id.guide_description_text)?.text =
                intent.getStringExtra(
                    FlutterLocalNotificationPluginsPlugin.EXTRA_OVERLAY_PERMISSION_GUIDE_DESC,
                )?.takeUnless { it.isBlank() }
                    ?.replaceAppNamePlaceholder(appName)
                    ?: getString(R.string.fln_overlay_permission_guide_description, appName)
        } catch (e: Exception) {
            Log.d("LocalNotificationPlugin", "bindAppInfo failed error=${e.message}")
        }
    }

    private fun String.replaceAppNamePlaceholder(appName: CharSequence): String {
        return replace("{n}", appName.toString())
    }

    private fun enableImmersiveStatusBar(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        }
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun resolveLayoutId(): Int {
        val layoutName =
            intent.getStringExtra(
                FlutterLocalNotificationPluginsPlugin.EXTRA_OVERLAY_PERMISSION_GUIDE_LAYOUT,
            )?.trim()
                ?.takeUnless { it.isBlank() }
                ?: return R.layout.fln_overlay_permission_guide
        val layoutId = resources.getIdentifier(layoutName, "layout", packageName)
        if (layoutId != 0) {
            return layoutId
        }
        Log.d(
            "LocalNotificationPlugin",
            "OverlayPermissionGuideActivity missing layout=$layoutName, fallback default",
        )
        return R.layout.fln_overlay_permission_guide
    }

    private fun findOptionalViewByName(idName: String): View? {
        val id = resources.getIdentifier(idName, "id", packageName)
        if (id == 0) {
            return null
        }
        return findViewById(id)
    }

    private fun animateRightDownIcon() {
        rightDownIconView
            ?.animate()
            ?.translationY(RIGHT_DOWN_TRANSLATION_PX)
            ?.setDuration(RIGHT_DOWN_ANIMATION_HALF_DURATION_MILLIS)
            ?.withEndAction {
                rightDownIconView
                    ?.animate()
                    ?.translationY(0f)
                    ?.setDuration(RIGHT_DOWN_ANIMATION_HALF_DURATION_MILLIS)
                    ?.start()
            }
            ?.start()
    }

    private fun animateSwitchIcon() {
        val view = switchView ?: return
        val nextResId = if (switchOn) R.drawable.switch_close else R.drawable.switch_open
        switchOn = !switchOn
        view.animate()
            .alpha(0f)
            .setDuration(SWITCH_FADE_DURATION_MILLIS)
            .withEndAction {
                view.setImageResource(nextResId)
                view.animate()
                    .alpha(1f)
                    .setDuration(SWITCH_FADE_DURATION_MILLIS)
                    .start()
            }
            .start()
    }

    companion object {
        private const val SWITCH_ANIMATION_INTERVAL_MILLIS = 800L
        private const val SWITCH_FADE_DURATION_MILLIS = 160L
        private const val RIGHT_DOWN_ANIMATION_INTERVAL_MILLIS = 700L
        private const val RIGHT_DOWN_ANIMATION_HALF_DURATION_MILLIS = 260L
        private const val RIGHT_DOWN_TRANSLATION_PX = 18f
    }
}
