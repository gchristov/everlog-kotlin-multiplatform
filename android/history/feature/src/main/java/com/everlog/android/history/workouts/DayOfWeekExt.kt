package com.everlog.android.history.workouts

import com.everlog.android.common.kotlin.WeekDay
import java.util.*
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.TemporalAdjusters

fun DayOfWeek.buildWeekDays(): List<WeekDay> {
    var now = LocalDate.now()
    now = now.with(TemporalAdjusters.previousOrSame(this))
    val days = ArrayList<WeekDay>()
    for (i in 0..6) {
        days.add(WeekDay(now.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), now))
        now = now.plusDays(1)
    }
    return days
}