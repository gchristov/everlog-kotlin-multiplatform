package com.everlog.utils

import com.everlog.data.model.WeekDay
import com.everlog.managers.preferences.SettingsManager
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.TemporalAdjusters
import java.util.*

fun DayOfWeek.buildWeekDays(): List<WeekDay> {
    var now = LocalDate.now()
    now = now.with(TemporalAdjusters.previousOrSame(SettingsManager.manager.firstDayOfWeek()))
    val days = ArrayList<WeekDay>()
    for (i in 0..6) {
        days.add(WeekDay(now.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), now))
        now = now.plusDays(1)
    }
    return days
}