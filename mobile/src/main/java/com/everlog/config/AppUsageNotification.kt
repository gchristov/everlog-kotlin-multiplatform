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

    fun getFirstTriggerAtMillis(): Long {
        val cal = Calendar.getInstance()
        cal[Calendar.HOUR_OF_DAY] = scheduleHourOfDay
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        cal.add(Calendar.DAY_OF_MONTH, scheduleIntervalDays)
        return cal.timeInMillis
    }

    fun getIntervalMillis(): Long {
        return TimeUnit.DAYS.toMillis(scheduleIntervalDays.toLong())
    }

    fun isValid(): Boolean {
        return (!TextUtils.isEmpty(title)
                && !TextUtils.isEmpty(description)
                && scheduleIntervalDays > 0
                && scheduleHourOfDay >= 0
                && scheduleHourOfDay <= 24)
    }
}