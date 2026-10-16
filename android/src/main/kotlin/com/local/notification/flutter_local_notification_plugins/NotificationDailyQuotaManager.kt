package com.local.notification.flutter_local_notification_plugins

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal enum class NotificationQuotaType {
    LOCAL,
    BROADCAST,
}

internal object NotificationDailyQuotaManager {
    private const val PREFS_NAME = "flutter_local_notification_plugins"
    private const val KEY_DATE = "notification_daily_quota_date"
    private const val KEY_LOCAL_COUNT = "notification_daily_local_count"
    private const val KEY_BROADCAST_COUNT = "notification_daily_broadcast_count"
    private val lock = Any()

    fun tryAcquire(
        context: Context,
        type: NotificationQuotaType,
        limit: Int,
    ): Boolean {
        if (limit <= 0) return true
        synchronized(lock) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val savedDate = prefs.getString(KEY_DATE, null)
            val key = if (type == NotificationQuotaType.LOCAL) KEY_LOCAL_COUNT else KEY_BROADCAST_COUNT
            val count = if (savedDate == today) prefs.getInt(key, 0) else 0
            if (count >= limit) return false
            val editor = prefs.edit()
            if (savedDate != today) {
                editor
                    .putString(KEY_DATE, today)
                    .putInt(KEY_LOCAL_COUNT, 0)
                    .putInt(KEY_BROADCAST_COUNT, 0)
            }
            editor.putInt(key, count + 1).commit()
            return true
        }
    }
}
