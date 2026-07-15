package com.local.notification.flutter_local_notification_plugins

import android.content.Context
import android.content.Intent
import android.util.Log

/** Keeps dynamically registered broadcast receivers alive for the application process. */
object BroadcastNotificationReceiverManager {
    private const val TAG = "BroadcastReceiverManager"
    private val lock = Any()

    fun replace(
        context: Context,
        actions: Set<String>,
    ) {
        val appContext = context.applicationContext
        synchronized(lock) {
            UnlockNotificationReceiver.removeAll(appContext)
            actions.forEach { action ->
                UnlockNotificationReceiver.add(
                    context = appContext,
                    action = action,
                    addPackageDataScheme = action == Intent.ACTION_PACKAGE_ADDED ||
                        action == Intent.ACTION_PACKAGE_REMOVED ||
                        action == Intent.ACTION_PACKAGE_REPLACED,
                )
            }
        }
        Log.d(TAG, "replace success count=${actions.size}")
    }

    fun disable(context: Context) {
        synchronized(lock) {
            UnlockNotificationReceiver.removeAll(context.applicationContext)
        }
        Log.d(TAG, "disable success")
    }
}
