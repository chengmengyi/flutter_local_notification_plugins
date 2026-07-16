package com.local.notification.flutter_local_notification_plugins

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object InProcessTimerManager {
    private const val TAG = "InProcessTimer"
    private const val MINUTE_MILLIS = 60_000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()

    @Volatile
    private var timerJob: Job? = null

    @Volatile
    private var minuteCount = 0L

    fun start(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            if (timerJob?.isActive == true) return
            minuteCount = 0L
            timerJob = scope.launch {
                var lastTickElapsed = SystemClock.elapsedRealtime()
                var elapsedRemainder = 0L
                Log.d(TAG, "start")
                while (isActive) {
                    delay(MINUTE_MILLIS)
                    val tickElapsed = SystemClock.elapsedRealtime()
                    val totalElapsed = elapsedRemainder + tickElapsed - lastTickElapsed
                    val elapsedMinutes = totalElapsed / MINUTE_MILLIS
                    elapsedRemainder = totalElapsed % MINUTE_MILLIS
                    lastTickElapsed = tickElapsed
                    if (elapsedMinutes <= 0L) continue
                    runCatching {
                        handleTick(appContext, elapsedMinutes)
                    }.onFailure {
                        Log.d(TAG, "tick failed error=${it.message}")
                    }
                }
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            timerJob?.cancel()
            timerJob = null
            minuteCount = 0L
        }
        Log.d(TAG, "stop")
    }

    fun isRunning(): Boolean = timerJob?.isActive == true

    private fun handleTick(context: Context, elapsedMinutes: Long) {
        val previousCount = minuteCount
        val currentCount = previousCount + elapsedMinutes
        minuteCount = currentCount
        val configs = LocalNotificationScheduler.timerWorkConfigs(context)
        Log.d(
            TAG,
            "tick count=$currentCount elapsedMinutes=$elapsedMinutes activeSlots=${configs.size}",
        )
        configs.forEach { config ->
            runCatching {
                val intervalMinutes = intervalMinutes(config.intervalMillis)
                val crossedBoundary =
                    previousCount / intervalMinutes < currentCount / intervalMinutes
                if (!crossedBoundary) return@runCatching
                val source = "in_process_timer_${config.slot}"
                val displayed =
                    LocalNotificationScheduler.tryDeliverFromTimerWork(
                        context,
                        config.scheduleId,
                        source,
                    )
                val foregroundRequested =
                    KeepAliveNotificationHelper.startOrUpdateForegroundService(context, source)
                Log.d(
                    TAG,
                    "trigger slot=${config.slot} scheduleId=${config.scheduleId} intervalMinutes=$intervalMinutes displayed=$displayed foregroundRequested=$foregroundRequested",
                )
            }.onFailure {
                Log.d(
                    TAG,
                    "trigger failed slot=${config.slot} scheduleId=${config.scheduleId} error=${it.message}",
                )
            }
        }
    }

    private fun intervalMinutes(intervalMillis: Long): Long {
        val fullMinutes = intervalMillis / MINUTE_MILLIS
        val hasRemainder = intervalMillis % MINUTE_MILLIS != 0L
        return (fullMinutes + if (hasRemainder) 1L else 0L).coerceAtLeast(1L)
    }
}
