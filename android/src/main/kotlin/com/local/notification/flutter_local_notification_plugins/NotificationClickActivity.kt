package com.local.notification.flutter_local_notification_plugins

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class NotificationClickActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleClick(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleClick(intent)
        finish()
    }

    private fun handleClick(intent: Intent?) {
        intent ?: return
        val arguments =
            mapOf(
                "id" to intent.getIntExtra(EXTRA_ID, 0),
                "title" to intent.getStringExtra(EXTRA_TITLE),
                "body" to intent.getStringExtra(EXTRA_BODY),
                "payload" to intent.getStringExtra(EXTRA_PAYLOAD),
                "payloadType" to (
                    intent.getStringExtra(EXTRA_PAYLOAD_TYPE)
                        ?: intent.getStringExtra(EXTRA_PAYLOAD)
                    ),
            )
        val dispatched =
            FlutterLocalNotificationPluginsPlugin.dispatchNotificationClicked(
                applicationContext,
                arguments,
            )
        if (!dispatched) {
            FlutterLocalNotificationPluginsPlugin.cacheLaunchDetails(
                applicationContext,
                arguments,
            )
        }
        startMainActivity()
    }

    private fun startMainActivity() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.putExtra(EXTRA_FROM_NOTIFICATION_CLICK, true)
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
        )
        startActivity(launchIntent)
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_PAYLOAD = "payload"
        const val EXTRA_PAYLOAD_TYPE = "payloadType"
        private const val EXTRA_FROM_NOTIFICATION_CLICK = "b03pdf.extra.FROM_NOTIFICATION_CLICK"
    }
}
