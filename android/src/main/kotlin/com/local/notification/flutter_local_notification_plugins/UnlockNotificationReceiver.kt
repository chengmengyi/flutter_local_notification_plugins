package com.local.notification.flutter_local_notification_plugins

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

class UnlockNotificationReceiver : BroadcastReceiver() {
    companion object {
        private val instances = mutableMapOf<String, UnlockNotificationReceiver>()

        fun add(
            context: Context,
            action: String,
            addPackageDataScheme: Boolean = false,
        ) {
            val key = if (addPackageDataScheme) "$action#package" else action
            if (instances.containsKey(key)) {
                return
            }
            val receiver = UnlockNotificationReceiver().apply {
                broadcastAction = action
            }
            val intentFilter =
                IntentFilter().apply {
                    addAction(action)
                    if (addPackageDataScheme) {
                        addDataScheme("package")
                    }
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    intentFilter,
                    Context.RECEIVER_NOT_EXPORTED,
                )
            } else {
                context.registerReceiver(receiver, intentFilter)
            }
            instances[key] = receiver
            Log.d(
                "LocalNotificationPlugin",
                "UnlockNotificationReceiver.add action=$action addPackageDataScheme=$addPackageDataScheme",
            )
        }

        fun removeAll(context: Context) {
            instances.values.forEach {
                try {
                    context.unregisterReceiver(it)
                } catch (_: Exception) {
                }
            }
            instances.clear()
            Log.d("LocalNotificationPlugin", "UnlockNotificationReceiver.removeAll")
        }
    }

    private var broadcastAction: String? = null

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (FlutterLocalNotificationPluginsPlugin.isNotificationBlocked(context)) {
            Log.d(
                "LocalNotificationPlugin",
                "UnlockNotificationReceiver blocked action=${intent.action}",
            )
            return
        }
        if (broadcastAction != null && broadcastAction != intent.action) {
            return
        }
        if (intent.action == Intent.ACTION_BATTERY_CHANGED && isInitialStickyBroadcast) {
            Log.d(
                "LocalNotificationPlugin",
                "UnlockNotificationReceiver.skipInitialBatteryChanged",
            )
            return
        }
        Log.d(
            "LocalNotificationPlugin",
            "UnlockNotificationReceiver.onReceive action=${intent.action}",
        )
        FlutterLocalNotificationPluginsPlugin.handleUnlockBroadcast(
            context,
            intent.action,
        )
    }
}
