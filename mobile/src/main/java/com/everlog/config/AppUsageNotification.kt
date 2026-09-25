package com.everlog.config

import android.text.TextUtils
import java.io.Serializable
import java.util.*
import java.util.concurrent.TimeUnit

data class AppUsageNotification (

        var title: String? = null,
        var description: String? = null,
        var scheduleIntervalDays: Int = 0,
        var scheduleHourOfDay: Int = 0

) : Serializable {

    fun getFirstTriggerAtMillis(fromMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = fromMillis
        cal[Calendar.HOUR_OF_DAY] = scheduleHourOfDay
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        cal.add(Calendar.DAY_OF_MONTH, scheduleIntervalDays)
        return cal.timeInMillis
    }

    fun getIntervalMillis(): Long {
        return TimeUnit.DAYS.toMillis(scheduleIntervalDays.toLong())
    }

    /**
     * Whether a reminder alarm firing at [now] should actually show. The alarm itself can't be trusted:
     * older app versions scheduled alarms that could never be cancelled, so they keep firing for users
     * who are active. Only show if the user has been away for the full interval since [lastActiveAt],
     * and at most once per half interval ([lastShownAt]) so a backlog of those alarms can't spam.
     * A [lastActiveAt] of 0 means we've never recorded activity, so the reminder shows. Times later
     * than [now] (the device clock was wrong, then corrected) are treated as never recorded, otherwise
     * they'd suppress reminders until real time catches up. The next visit or reminder overwrites them.
     */
    fun shouldShow(lastActiveAt: Long, lastShownAt: Long, now: Long): Boolean {
        val active = if (lastActiveAt > now) 0 else lastActiveAt
        val shown = if (lastShownAt > now) 0 else lastShownAt
        if (active > 0 && now < getFirstTriggerAtMillis(active)) {
            // User was active since this reminder was scheduled
            return false
        }
        return shown < active || now - shown >= getIntervalMillis() / 2
    }

    fun isValid(): Boolean {
        return (!TextUtils.isEmpty(title)
                && !TextUtils.isEmpty(description)
                && scheduleIntervalDays > 0
                && scheduleHourOfDay >= 0
                && scheduleHourOfDay <= 24)
    }
}
