package com.example.aaaaketahuan.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * Handles scheduling and cancellation of daily reminder alarms.
 *
 * Uses [AlarmManager.setAlarmClock] for exact delivery on modern Android
 * (API 31+), falling back to [AlarmManager.set] if exact alarm permission
 * is not available on certain devices/ROMs.
 * Re-schedules the next alarm each time it fires via [ReminderReceiver].
 */
object ReminderScheduler {

    private const val ACTION_REMINDER = "com.example.aaaaketahuan.ACTION_DAILY_REMINDER"
    private const val REQUEST_CODE = 2001
    private const val TAG = "ReminderScheduler"

    /**
     * Schedules the next daily alarm at [hour]:[minute].
     * If that time has already passed today, schedules for tomorrow instead.
     */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Compute next alarm time
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            // setAlarmClock is the most reliable on modern Android.
            // It does NOT require SCHEDULE_EXACT_ALARM permission.
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Some manufacturer ROMs (Xiaomi, Huawei, Oppo, etc.) block
            // setAlarmClock without additional permissions. Fall back to
            // inexact set() to avoid crashing the app.
            Log.w(TAG, "setAlarmClock di-blokir, fallback ke set(): ${e.message}")
            fallbackSchedule(alarmManager, calendar.timeInMillis, pendingIntent)
        } catch (e: RuntimeException) {
            // Catch-all for any other unexpected runtime errors
            Log.w(TAG, "setAlarmClock gagal, fallback ke set(): ${e.message}")
            fallbackSchedule(alarmManager, calendar.timeInMillis, pendingIntent)
        }
    }

    /** Fallback: use inexact [AlarmManager.set] when exact alarm is unavailable. */
    private fun fallbackSchedule(
        alarmManager: AlarmManager,
        triggerTimeMillis: Long,
        operation: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                operation
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                operation
            )
        }
    }

    /**
     * Cancels any existing daily reminder alarm.
     */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Reschedules the alarm (cancel existing + schedule new).
     */
    fun reschedule(context: Context, hour: Int, minute: Int) {
        cancel(context)
        schedule(context, hour, minute)
    }
}

/**
 * BroadcastReceiver that receives the daily alarm and shows notification.
 * Also re-schedules the alarm for the next day.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.createChannel(context)
        NotificationHelper.showReminder(context)

        // Re-schedule for the same time tomorrow (the user's reminder time)
        // This replaces the old setRepeating() approach which is inexact on API 31+
        val prefs = context.getSharedPreferences("aaaaketahuan_config", Context.MODE_PRIVATE)
        val hour = prefs.getInt("reminder_hour", 20)
        val minute = prefs.getInt("reminder_minute", 0)
        ReminderScheduler.schedule(context, hour, minute)
    }
}
