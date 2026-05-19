package com.local.notification.flutter_local_notification_plugins

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView

class OverlayPermissionGuideActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var switchOn = false
    private var switchView: ImageView? = null
    private val switchRunnable =
        object : Runnable {
            override fun run() {
                animateSwitchIcon()
                handler.postDelayed(this, SWITCH_ANIMATION_INTERVAL_MILLIS)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LocalNotificationPlugin", "OverlayPermissionGuideActivity onCreate")
        overridePendingTransition(0, 0)
        setContentView(R.layout.fln_overlay_permission_guide)
        bindAppInfo()
        switchView = findViewById(R.id.icon_switch)
        switchView?.setImageResource(R.drawable.switch_close)
        handler.postDelayed(switchRunnable, SWITCH_ANIMATION_INTERVAL_MILLIS)
        findViewById<View>(R.id.fln_overlay_permission_guide_root)?.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(switchRunnable)
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
            findViewById<TextView>(R.id.guide_description_text)?.text =
                getString(R.string.fln_overlay_permission_guide_description, appName)
        } catch (e: Exception) {
            Log.d("LocalNotificationPlugin", "bindAppInfo failed error=${e.message}")
        }
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
    }
}
