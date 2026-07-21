package com.example.aaaaketahuan

import android.app.Application
import com.example.aaaaketahuan.util.NotificationHelper
import com.example.aaaaketahuan.util.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AaaaKetahuanApp : Application() {

    @Inject
    lateinit var repository: com.example.aaaaketahuan.data.repository.TransaksiRepository

    override fun onCreate() {
        super.onCreate()
        // Create notification channel for daily reminder
        NotificationHelper.createChannel(this)
        // Re-schedule active reminder alarm after device reboot
        repository.refreshReminderAlarm()
    }
}
