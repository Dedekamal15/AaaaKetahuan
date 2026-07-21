package com.example.aaaaketahuan.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-schedules the daily reminder alarm after device reboot.
 * Without this, all [AlarmManager] alarms are lost on reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("aaaaketahuan_config", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("reminder_enabled", false)
            if (enabled) {
                val hour = prefs.getInt("reminder_hour", 20)
                val minute = prefs.getInt("reminder_minute", 0)
                ReminderScheduler.schedule(context, hour, minute)
            }
        }
    }
}
