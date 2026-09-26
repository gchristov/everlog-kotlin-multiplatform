package com.everlog.managers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import com.everlog.R
import com.everlog.application.ELApplication
import com.everlog.config.AppUsageNotification
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.AppLaunchManager
import com.everlog.managers.preferences.PreferencesManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.receivers.AppUsageReceiver
import com.everlog.services.fcm.ELFirebaseMessagingService
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Reminds the user to come back when they haven't been in the app for a while. A single alarm is set
 * for the next reminder when they leave the home screen, and cancelled when they return. Each reminder
 * that shows sets the alarm for the next one, further out, until [AppUsageNotification.maxReminders].
 */
class AppUsageReminderManager : PreferencesManager() {

    private enum class PreferenceKeys {
        APP_USAGE_LAST_SHOWN_AT,
        APP_USAGE_SHOWN_COUNT,
    }

    companion object {

        const val TAG = "AppUsageReminderManager"
        // Set on the notification's open app intent, so the open can be attributed to the reminder
        const val EXTRA_REMINDER_ATTEMPT = "EXTRA_APP_USAGE_REMINDER_ATTEMPT"
        const val EXTRA_REMINDER_TITLE = "EXTRA_APP_USAGE_REMINDER_TITLE"
        private const val NOTIFICATION_ID = 0x1610
        // Must stay fixed so cancel() and re-scheduling match the pending alarm
        private const val ALARM_REQUEST_CODE = NOTIFICATION_ID
        // Plain set() lets Android deliver up to 75% of the time until the alarm late, days for a weekly
        // reminder. A window keeps it close to the configured hour without needing the exact alarm permission.
        private val ALARM_WINDOW_MILLIS = TimeUnit.HOURS.toMillis(1)

        private val preferences = AppUsageReminderManager()

        /**
         * The user is leaving the home screen: record that they were active and set the alarm for the
         * first reminder.
         */
        @JvmStatic
        @JvmOverloads
        fun schedule(now: Long = System.currentTimeMillis()) {
            AppLaunchManager.manager.recordActivity(Date(now))
            scheduleNext(now)
        }

        /**
         * Sets the alarm again without recording activity, e.g. after a reboot or app update, which
         * clear or replace it.
         */
        @JvmStatic
        @JvmOverloads
        fun reschedule(now: Long = System.currentTimeMillis()) {
            scheduleNext(now)
        }

        /**
         * The alarm went off. It can't be trusted to mean a reminder is due: the user may have been
         * active since it was set, and older app versions set alarms that can't be cancelled, which
         * keep firing. So only show if one is due, then set the alarm for the next one.
         */
        @JvmStatic
        @JvmOverloads
        fun onAlarm(now: Long = System.currentTimeMillis()) {
            val reminder = nextReminder(now)
            if (reminder?.isDue(now) == true && !notificationsEnabled()) {
                // Don't count a reminder the user can't see. Leaving the home screen sets the alarm again.
                Timber.tag(TAG).i("Skipped app usage reminder - notifications disabled")
                cancelAlarm()
                return
            }
            if (reminder?.isDue(now) == true) {
                ELFirebaseMessagingService.notify(NOTIFICATION_ID, reminder.title, reminder.description, Bundle().apply {
                    putInt(EXTRA_REMINDER_ATTEMPT, reminder.attempt)
                    putString(EXTRA_REMINDER_TITLE, reminder.title)
                })
                preferences.setShown(now, reminder.attempt)
                AnalyticsManager.manager.appUsageReminderShown(reminder.attempt, reminder.title)
                Timber.tag(TAG).i("Showed app usage reminder: attempt=%s", reminder.attempt)
            } else {
                Timber.tag(TAG).i("Skipped app usage reminder: due=%s", reminder?.let { Date(it.dueAtMillis) })
            }
            scheduleNext(now)
        }

        @JvmStatic
        fun cancel() {
            // Cancel notification
            ELNotificationManager.cancel(NOTIFICATION_ID)
            // Cancel future reminder
            cancelAlarm()
            Timber.tag(TAG).i("Cancelled app usage reminder")
        }

        private fun scheduleNext(now: Long) {
            cancelAlarm()
            val schedule = getSchedule() ?: return
            val reminder = nextReminder(now) ?: return
            val alarmAt = reminder.alarmAtMillis(now, schedule.scheduleHourOfDay)
            val alarmManager = ELApplication.getInstance().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, alarmAt, ALARM_WINDOW_MILLIS, buildAlarmIntent())
            Timber.tag(TAG).i("Scheduled app usage reminder: attempt=%s trigger=%s", reminder.attempt, Date(alarmAt))
        }

        private fun nextReminder(now: Long): AppUsageNotification.Reminder? {
            if (!SettingsManager.manager.loggedIn()) {
                Timber.tag(TAG).i("No app usage reminder - logged out")
                return null
            }
            val schedule = getSchedule()
            if (schedule == null) {
                Timber.tag(TAG).i("No app usage reminder - schedule invalid")
                return null
            }
            val reminder = schedule.nextReminder(
                    lastActiveAt = AppLaunchManager.manager.lastActiveDate(),
                    lastShownAt = preferences.lastShownAt(),
                    shownCount = preferences.shownCount(),
                    now = now)
            if (reminder == null) {
                Timber.tag(TAG).i("No app usage reminder - none left until the user is back")
            }
            return reminder
        }

        private fun notificationsEnabled(): Boolean {
            val manager = NotificationManagerCompat.from(ELApplication.getInstance())
            if (!manager.areNotificationsEnabled()) {
                return false
            }
            // The user can also turn off just the channel the reminder is posted to
            val channel = manager.getNotificationChannelCompat(ELApplication.getInstance().getString(R.string.notification_channel_normal))
            return channel == null || channel.importance != NotificationManagerCompat.IMPORTANCE_NONE
        }

        private fun cancelAlarm() {
            val alarmManager = ELApplication.getInstance().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(buildAlarmIntent())
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

    private fun lastShownAt(): Long {
        return getPreference(PreferenceKeys.APP_USAGE_LAST_SHOWN_AT.name, 0L)
    }

    private fun shownCount(): Int {
        return getPreference(PreferenceKeys.APP_USAGE_SHOWN_COUNT.name, 0)
    }

    private fun setShown(at: Long, count: Int) {
        savePreference(at, PreferenceKeys.APP_USAGE_LAST_SHOWN_AT.name)
        savePreference(count, PreferenceKeys.APP_USAGE_SHOWN_COUNT.name)
    }
}
