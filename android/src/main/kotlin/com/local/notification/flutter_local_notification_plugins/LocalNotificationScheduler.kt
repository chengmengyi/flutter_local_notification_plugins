package com.local.notification.flutter_local_notification_plugins

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/** Coordinates persisted local-notification schedules across AlarmManager and WorkManager. */
object LocalNotificationScheduler {
    private const val TAG = "LocalNotificationScheduler"
    private const val PREFS_NAME = "flutter_local_notification_plugins"
    private const val KEY_SCHEDULES = "local_notification_schedules_v1"
    private const val KEY_DISPLAY_SEQUENCE = "local_notification_display_sequence"
    private const val EXTRA_SCHEDULE_ID = "local_notification_schedule_id"
    private const val EXTRA_SCHEDULE_VERSION = "local_notification_schedule_version"
    private const val MIN_INTERVAL_MILLIS = 1_000L
    private val lock = Any()

    private data class Schedule(
        val id: Int,
        val version: Long,
        val workSlot: Int,
        val intervalMillis: Long,
        val nextTriggerAt: Long,
        val lastDeliveredOccurrence: Long,
        val lastDeliveredAt: Long,
        val extras: String,
    )

    data class TimerWorkConfig(
        val slot: Int,
        val scheduleId: Int,
        val intervalMillis: Long,
    )

    fun register(context: Context, sourceIntent: Intent) {
        val id = sourceIntent.getIntExtra("id", 0)
        val interval = sourceIntent.getLongExtra("repeatIntervalMilliseconds", 0L)
        if (id == 0 || interval <= 0L) return
        val appContext = context.applicationContext
        val now = System.currentTimeMillis()
        val normalizedInterval = interval.coerceAtLeast(MIN_INTERVAL_MILLIS)
        val encodedExtras = encodeBundle(sourceIntent.extras ?: Bundle())
        val scheduleResult = synchronized(lock) {
            val schedules = readSchedules(appContext)
            val old = schedules[id]
            val workSlot =
                old?.workSlot?.takeIf { it in 1..3 }
                    ?: firstAvailableWorkSlot(schedules.values)
            val changed =
                old == null ||
                    old.intervalMillis != normalizedInterval ||
                    old.extras != encodedExtras
            val updated =
                if (old != null &&
                    old.intervalMillis == normalizedInterval &&
                    old.extras == encodedExtras
                ) {
                    old.copy(workSlot = workSlot)
                } else {
                    Schedule(
                        id = id,
                        version = (old?.version ?: 0L) + 1L,
                        workSlot = workSlot,
                        intervalMillis = normalizedInterval,
                        nextTriggerAt = now + normalizedInterval,
                        lastDeliveredOccurrence = 0L,
                        lastDeliveredAt = 0L,
                        extras = encodedExtras,
                    )
                }
            updated.also {
                schedules[id] = it
                writeSchedules(appContext, schedules)
            } to changed
        }
        val schedule = scheduleResult.first
        scheduleAlarm(appContext, schedule)
        TimerNotificationWorkManager.schedule(appContext, schedule.id, update = scheduleResult.second)
        FixedTimerAlarmManager.schedule(appContext, schedule.id, update = scheduleResult.second)
        InProcessTimerManager.start(appContext)
    }

    fun handleAlarm(context: Context, intent: Intent): Boolean {
        val scheduleId = intent.getIntExtra(EXTRA_SCHEDULE_ID, 0)
        if (scheduleId == 0) return false
        deliverIfDue(
            context = context.applicationContext,
            scheduleId = scheduleId,
            expectedVersion = intent.getLongExtra(EXTRA_SCHEDULE_VERSION, -1L),
            source = "alarm",
        )
        return true
    }

    fun hasSchedules(context: Context): Boolean =
        synchronized(lock) { readSchedules(context.applicationContext).isNotEmpty() }

    fun timerWorkConfigs(context: Context): List<TimerWorkConfig> =
        synchronized(lock) {
            val appContext = context.applicationContext
            val schedules = readSchedules(appContext)
            var changed = false
            schedules.values.filter { it.workSlot !in 1..3 }.forEach { schedule ->
                val slot = firstAvailableWorkSlot(schedules.values)
                if (slot != 0) {
                    schedules[schedule.id] = schedule.copy(workSlot = slot)
                    changed = true
                }
            }
            if (changed) writeSchedules(appContext, schedules)
            schedules.values
                .filter { it.workSlot in 1..3 }
                .map { TimerWorkConfig(it.workSlot, it.id, it.intervalMillis) }
                .sortedBy { it.slot }
        }

    fun tryDeliverFromTimerWork(context: Context, scheduleId: Int, source: String): Boolean {
        val appContext = context.applicationContext
        val claimed = synchronized(lock) {
            val schedules = readSchedules(appContext)
            val current = schedules[scheduleId] ?: return false
            val now = System.currentTimeMillis()
            if (!canDeliver(current, now)) return false
            val updated = current.copy(lastDeliveredAt = now)
            schedules[scheduleId] = updated
            writeSchedules(appContext, schedules)
            decodeBundle(current.extras)
        }
        val notificationIntent = Intent(appContext, LocalNotificationReceiver::class.java).apply {
            replaceExtras(claimed)
            putExtra("deliverySource", source)
        }
        FlutterLocalNotificationPluginsPlugin.showNotificationFromIntent(appContext, notificationIntent)
        Log.d(TAG, "delivered scheduleId=$scheduleId source=$source")
        return true
    }

    fun reconcile(context: Context) {
        val appContext = context.applicationContext
        val snapshot = synchronized(lock) { readSchedules(appContext).values.toList() }
        snapshot.forEach { schedule ->
            if (schedule.nextTriggerAt <= System.currentTimeMillis()) {
                deliverIfDue(appContext, schedule.id, schedule.version, "work_manager")
            } else {
                scheduleAlarm(appContext, schedule)
            }
        }
    }

    fun restore(context: Context) {
        val appContext = context.applicationContext
        val snapshot = synchronized(lock) { readSchedules(appContext).values.toList() }
        snapshot.forEach { schedule ->
            if (schedule.nextTriggerAt <= System.currentTimeMillis()) {
                deliverIfDue(appContext, schedule.id, schedule.version, "restore")
            } else {
                scheduleAlarm(appContext, schedule)
            }
        }
        Log.d(TAG, "restore count=${snapshot.size}")
        TimerNotificationWorkManager.restore(appContext)
        FixedTimerAlarmManager.restore(appContext)
        InProcessTimerManager.start(appContext)
    }

    fun nextDisplayId(context: Context): Int {
        synchronized(lock) {
            val prefs = prefs(context)
            val current = prefs.getInt(KEY_DISPLAY_SEQUENCE, 100_000)
            val next = if (current >= Int.MAX_VALUE - 1) 100_000 else current + 1
            prefs.edit().putInt(KEY_DISPLAY_SEQUENCE, next).commit()
            return next
        }
    }

    private fun deliverIfDue(
        context: Context,
        scheduleId: Int,
        expectedVersion: Long,
        source: String,
    ) {
        if (scheduleId == 0) return
        val claimed = synchronized(lock) {
            val schedules = readSchedules(context)
            val current = schedules[scheduleId] ?: return
            if (current.version != expectedVersion) return
            val now = System.currentTimeMillis()
            if (current.nextTriggerAt > now) {
                scheduleAlarm(context, current)
                return
            }
            val occurrence = current.nextTriggerAt
            if (current.lastDeliveredOccurrence == occurrence) return
            val shouldDisplay = canDeliver(current, now)
            var next = occurrence + current.intervalMillis
            while (next <= now) next += current.intervalMillis
            val advanced = current.copy(
                nextTriggerAt = next,
                lastDeliveredOccurrence = occurrence,
                lastDeliveredAt = if (shouldDisplay) now else current.lastDeliveredAt,
            )
            schedules[scheduleId] = advanced
            writeSchedules(context, schedules)
            advanced to if (shouldDisplay) decodeBundle(current.extras) else null
        }
        scheduleAlarm(context, claimed.first)
        val extras = claimed.second ?: return
        val notificationIntent = Intent(context, LocalNotificationReceiver::class.java).apply {
            replaceExtras(extras)
            putExtra("deliverySource", source)
        }
        FlutterLocalNotificationPluginsPlugin.showNotificationFromIntent(context, notificationIntent)
        Log.d(TAG, "delivered scheduleId=$scheduleId source=$source")
    }

    private fun scheduleAlarm(context: Context, schedule: Schedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = createPendingIntent(context, schedule)
        alarmManager.cancel(pendingIntent)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, schedule.nextTriggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, schedule.nextTriggerAt, pendingIntent)
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, schedule.nextTriggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, schedule.nextTriggerAt, pendingIntent)
            }
        }
    }

    private fun createPendingIntent(context: Context, schedule: Schedule): PendingIntent {
        val intent = Intent(context, LocalNotificationReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_SCHEDULE_VERSION, schedule.version)
        }
        return PendingIntent.getBroadcast(
            context,
            schedule.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun readSchedules(context: Context): MutableMap<Int, Schedule> {
        val raw = prefs(context).getString(KEY_SCHEDULES, null) ?: return mutableMapOf()
        return runCatching {
            val array = JSONArray(raw)
            buildMap {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val schedule = Schedule(
                        id = item.getInt("id"),
                        version = item.getLong("version"),
                        workSlot = item.optInt("workSlot"),
                        intervalMillis = item.getLong("intervalMillis"),
                        nextTriggerAt = item.getLong("nextTriggerAt"),
                        lastDeliveredOccurrence = item.optLong("lastDeliveredOccurrence"),
                        lastDeliveredAt = item.optLong("lastDeliveredAt"),
                        extras = item.getString("extras"),
                    )
                    put(schedule.id, schedule)
                }
            }.toMutableMap()
        }.getOrElse {
            Log.d(TAG, "readSchedules failed error=${it.message}")
            mutableMapOf()
        }
    }

    private fun writeSchedules(context: Context, schedules: Map<Int, Schedule>) {
        val array = JSONArray()
        schedules.values.forEach { schedule ->
            array.put(JSONObject().apply {
                put("id", schedule.id)
                put("version", schedule.version)
                put("workSlot", schedule.workSlot)
                put("intervalMillis", schedule.intervalMillis)
                put("nextTriggerAt", schedule.nextTriggerAt)
                put("lastDeliveredOccurrence", schedule.lastDeliveredOccurrence)
                put("lastDeliveredAt", schedule.lastDeliveredAt)
                put("extras", schedule.extras)
            })
        }
        prefs(context).edit().putString(KEY_SCHEDULES, array.toString()).commit()
    }

    private fun encodeBundle(bundle: Bundle): String {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(bundle)
            Base64.encodeToString(parcel.marshall(), Base64.NO_WRAP)
        } finally {
            parcel.recycle()
        }
    }

    private fun decodeBundle(value: String): Bundle {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        val parcel = Parcel.obtain()
        return try {
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            parcel.readBundle(LocalNotificationScheduler::class.java.classLoader) ?: Bundle()
        } finally {
            parcel.recycle()
        }
    }

    private fun canDeliver(schedule: Schedule, now: Long): Boolean {
        if (schedule.lastDeliveredAt <= 0L) return true
        val cooldown = (schedule.intervalMillis * 85L / 100L).coerceAtLeast(30_000L)
        return now - schedule.lastDeliveredAt >= cooldown
    }

    private fun firstAvailableWorkSlot(schedules: Collection<Schedule>): Int {
        val occupied = schedules.map { it.workSlot }.toSet()
        return (1..3).firstOrNull { it !in occupied } ?: 0
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
