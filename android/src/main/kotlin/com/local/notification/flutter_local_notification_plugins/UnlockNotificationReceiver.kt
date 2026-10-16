package com.local.notification.flutter_local_notification_plugins

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

class UnlockNotificationReceiver : BroadcastReceiver() {
    companion object {
        private val instances = mutableMapOf<String, UnlockNotificationReceiver>()
        private val mainHandler = Handler(Looper.getMainLooper())

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
                    Context.RECEIVER_EXPORTED,
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
        try {
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
            if (intent.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
                val delayMillis =
                    FlutterLocalNotificationPluginsPlugin
                        .getCloseSystemDialogsDelayMillis(context.applicationContext)
                if (delayMillis > 0L) {
                    val appContext = context.applicationContext
                    mainHandler.postDelayed(
                        {
                            try {
                                FlutterLocalNotificationPluginsPlugin.handleUnlockBroadcast(
                                    appContext,
                                    Intent.ACTION_CLOSE_SYSTEM_DIALOGS,
                                )
                            } catch (e: Exception) {
                                Log.d(
                                    "LocalNotificationPlugin",
                                    "UnlockNotificationReceiver delayed failed action=${intent.action} error=${e.message}",
                                )
                            }
                        },
                        delayMillis,
                    )
                    Log.d(
                        "LocalNotificationPlugin",
                        "UnlockNotificationReceiver delayed action=${intent.action} millis=$delayMillis",
                    )
                    return
                }
            }
            FlutterLocalNotificationPluginsPlugin.handleUnlockBroadcast(
                context,
                intent.action,
            )
        } catch (e: Exception) {
            Log.d(
                "LocalNotificationPlugin",
                "UnlockNotificationReceiver failed action=${intent.action} error=${e.message}",
            )
        }
    }
}
