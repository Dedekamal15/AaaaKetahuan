package com.example.aaaaketahuan.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.aaaaketahuan.MainActivity
import com.example.aaaaketahuan.R

object NotificationHelper {

    private const val CHANNEL_ID = "daily_reminder"
    private const val CHANNEL_NAME = "Pengingat Harian"
    private const val CHANNEL_DESC = "Pengingat untuk mencatat transaksi harian"
    private const val NOTIFICATION_ID = 1001

    /**
     * Creates the notification channel (required for Android 8+).
     * Safe to call multiple times — Android ignores duplicate channels.
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESC
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * Shows the daily reminder notification.
     * Tapping the notification opens the main activity.
     */
    fun showReminder(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Catat Transaksi Hari Ini")
            .setContentText("Jangan lupa mencatat pemasukan atau pengeluaran Anda hari ini!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted on Android 13+
        }
    }

    /**
     * Cancels any existing reminder notification.
     */
    fun cancelReminder(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
