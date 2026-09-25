package com.everlog.managers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.everlog.application.ELApplication
import com.everlog.config.AppUsageNotification
import com.everlog.managers.preferences.PreferencesManager
import com.everlog.receivers.AppUsageReceiver
import com.everlog.services.fcm.ELFirebaseMessagingService
import timber.log.Timber
import java.util.*

class AppUsageReminderManager : PreferencesManager() {

    private enum class PreferenceKeys {
        APP_USAGE_LAST_ACTIVE_AT,
        APP_USAGE_LAST_SHOWN_AT,
    }

    companion object {

        const val TAG = "AppUsageReminderManager"
        private const val NOTIFICATION_ID = 0x1610
        // Must stay fixed so cancel() and re-scheduling match the pending alarm
        private const val ALARM_REQUEST_CODE = NOTIFICATION_ID

        private val preferences = AppUsageReminderManager()

        @JvmStatic
        @JvmOverloads
        fun showNotification(now: Long = System.currentTimeMillis()) {
            val schedule = getSchedule()
            if (schedule == null) {
                Timber.tag(TAG).i("Could not show app usage notification - schedule invalid")
                return
            }
            val lastActiveAt = preferences.lastActiveAt()
            val lastShownAt = preferences.lastShownAt()
            if (!schedule.shouldShow(lastActiveAt, lastShownAt, now)) {
                Timber.tag(TAG).i("Skipped app usage notification: lastActive=%s lastShown=%s", Date(lastActiveAt), Date(lastShownAt))
                return
            }
            ELFirebaseMessagingService.notify(NOTIFICATION_ID, schedule.title!!, schedule.description!!)
            preferences.setLastShownAt(now)
            Timber.tag(TAG).i("Showed app usage notification")
        }

        @JvmStatic
        @JvmOverloads
        fun schedule(now: Long = System.currentTimeMillis()) {
            // The user is leaving the app, so this is the latest point they were active
            preferences.setLastActiveAt(now)
            val schedule = getSchedule()
            if (schedule != null) {
                cancel()
                val alarmManager = ELApplication.getInstance().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                        schedule.getFirstTriggerAtMillis(now),
                        schedule.getIntervalMillis(),
                        buildAlarmIntent())
                Timber.tag(TAG).i("Scheduled pending app usage notification: trigger=%s intervalDays=%s", Date(schedule.getFirstTriggerAtMillis(now)), schedule.scheduleIntervalDays)
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
                ALARM_REQUEST_CODE,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun getSchedule(): AppUsageNotification? {
            return RemoteConfigManager.manager.notificationAppUsage()
        }
    }

    private fun lastActiveAt(): Long {
        return getPreference(PreferenceKeys.APP_USAGE_LAST_ACTIVE_AT.name, 0L)
    }

    private fun setLastActiveAt(value: Long) {
        savePreference(value, PreferenceKeys.APP_USAGE_LAST_ACTIVE_AT.name)
    }

    private fun lastShownAt(): Long {
        return getPreference(PreferenceKeys.APP_USAGE_LAST_SHOWN_AT.name, 0L)
    }

    private fun setLastShownAt(value: Long) {
        savePreference(value, PreferenceKeys.APP_USAGE_LAST_SHOWN_AT.name)
    }
}
