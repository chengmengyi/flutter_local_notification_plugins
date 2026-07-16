package com.local.notification.flutter_local_notification_plugins

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class FixedTimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            FixedTimerAlarmManager.handleAlarm(context, intent)
        }.onFailure {
            Log.d(TAG, "onReceive failed action=${intent.action} error=${it.message}")
        }
    }

    companion object {
        private const val TAG = "FixedTimerAlarmReceiver"
    }
}
