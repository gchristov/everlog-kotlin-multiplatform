package com.everlog.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.everlog.managers.AppUsageReminderManager
import timber.log.Timber

class AppUsageReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            // A reboot clears the alarm and an update keeps the old one, so set it again
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Timber.tag(AppUsageReminderManager.TAG).i("Received %s, rescheduling app usage reminder", intent.action)
                AppUsageReminderManager.reschedule()
            }
            else -> {
                Timber.tag(AppUsageReminderManager.TAG).i("Received request to show app usage reminder")
                AppUsageReminderManager.onAlarm()
            }
        }
    }
}
