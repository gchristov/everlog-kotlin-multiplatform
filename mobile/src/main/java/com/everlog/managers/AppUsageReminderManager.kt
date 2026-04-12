package com.everlog.managers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.everlog.application.ELApplication
import com.everlog.config.AppUsageNotification
import com.everlog.receivers.AppUsageReceiver
import com.everlog.services.fcm.ELFirebaseMessagingService
import timber.log.Timber
import java.util.*
import kotlin.random.Random

class AppUsageReminderManager {

    companion object {

        const val TAG = "AppUsageReminderManager"
        private const val NOTIFICATION_ID = 0x1610

        @JvmStatic
        fun showNotification() {
            val schedule = getSchedule()
            if (schedule != null) {
                ELFirebaseMessagingService.notify(NOTIFICATION_ID, schedule.title!!, schedule.description!!)
                Timber.tag(TAG).i("Showed app usage notification")
            } else {
                Timber.tag(TAG).i("Could not show app usage notification - schedule invalid")
            }
        }

        @JvmStatic
        fun schedule() {
            val schedule = getSchedule()
            if (schedule != null) {
                cancel()
                val alarmManager = ELApplication.getInstance().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                        schedule.getFirstTriggerAtMillis(),
                        schedule.getIntervalMillis(),
                        buildAlarmIntent())
                Timber.tag(TAG).i("Scheduled pending app usage notification: trigger=%s intervalDays=%s", Date(schedule.getFirstTriggerAtMillis()), schedule.scheduleIntervalDays)
            } else {
                Timber.tag(TAG).i("Could not show app usage notification - schedule invalid")
            }
        }

        @JvmStatic
        fun cancel() {
            // Cancel notification
            ELNotificationManager.cancel(NOTIFICATION_ID)
            // Cancel future reminder
            val alarmManager = ELApplication.getInstance().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(buildAlarmIntent())
            Timber.tag(TAG).i("Cancelled pending app usage notification")
        }

        private fun buildAlarmIntent(): PendingIntent {
            val notificationIntent = Intent(ELApplication.getInstance(), AppUsageReceiver::class.java)
            return PendingIntent.getBroadcast(
                ELApplication.getInstance(),
                Random.nextInt(),
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        private fun getSchedule(): AppUsageNotification? {
            return RemoteConfigManager.manager.notificationAppUsage()
        }
    }
}