package com.everlog.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.everlog.managers.AppUsageReminderManager
import timber.log.Timber

class AppUsageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.tag(AppUsageReminderManager.TAG).i("Received request to show app usage reminder")
        AppUsageReminderManager.showNotification()
    }
}