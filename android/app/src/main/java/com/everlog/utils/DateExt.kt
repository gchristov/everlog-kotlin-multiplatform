package com.everlog.utils

import com.everlog.managers.preferences.SettingsManager
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.WeekFields
import java.util.*

fun Date.timestampWithAddedDays(days: Int): Long {
    return this.toLocalDate().atStartOfDay().plusDays(days.toLong()).toInstant(ZoneOffset.UTC).toEpochMilli()
}

fun Date.calendarDaysDifference(another: Date): Int {
    val date1 = this.toLocalDate()
    val date2 = another.toLocalDate()
    return date1.until(date2).days
}

fun Date.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this.time).atZone(ZoneId.systemDefault()).toLocalDate()
}

fun Date.toLocalDateTime(): LocalDateTime {
    return Instant.ofEpochMilli(this.time).atZone(ZoneId.systemDefault()).toLocalDateTime()
}

fun Date.toDayOfMonthWithNoHourTimestamp(): Long {
    val then = this.toLocalDate()
    return then.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}

fun Date.toDayOfMonthWithMonthOnlyTimestamp(): Long {
    var then = this.toLocalDate()
    then = then.withDayOfMonth(1)
    return then.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}

fun Date.toDayOfMonthWithYearOnlyTimestamp(): Long {
    var then = this.toLocalDate()
    then = then.withDayOfMonth(1)
    then = then.withMonth(1)
    return then.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}

fun Date.isFuture(): Boolean {
    val now = this.toLocalDate()
    val then = Date().toLocalDate()
    return now.isAfter(then)
}

fun Date.isSameYear(time: Long): Boolean {
    val now = this.toLocalDate()
    val then = Date(time).toLocalDate()
    return now.year == then.year
}

fun Date.isSameMonth(time: Long): Boolean {
    val now = this.toLocalDate()
    val then = Date(time).toLocalDate()
    return now.month == then.month && this.isSameYear(time)
}

fun Date.isSameWeek(time: Long) : Boolean {
    val sundayStart = SettingsManager.manager.firstDayOfWeek() == DayOfWeek.SUNDAY
    val woy = (if (sundayStart) WeekFields.SUNDAY_START else WeekFields.ISO).weekOfWeekBasedYear()
    // Calculate current week number for each date.
    val now = this.toLocalDate()
    val nowWeekNumber = now.get(woy)
    val then = Date(time).toLocalDate()
    val thenWeekNumber = then.get(woy)
    return nowWeekNumber == thenWeekNumber && this.isSameYear(time)
}

fun Date.isSameDay(time: Long) : Boolean {
    val now = this.toLocalDate()
    val then = Date(time).toLocalDate()
    return now.dayOfMonth == then.dayOfMonth && this.isSameMonth(time) && this.isSameYear(time)
}

fun Date.timeOfDayMessage(): String {
    val c = Calendar.getInstance()
    c.time = this
    val timeOfDay = c.get(Calendar.HOUR_OF_DAY)

    if (timeOfDay in 0..11) {
        return "Morning"
    } else if (timeOfDay in 12..15) {
        return "Afternoon"
    }
    return "Evening"
}

fun Date.dayOfWeekFormatted(): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, d LLL")
    return formatter.format(this.toLocalDate())
}

fun Date.workoutFormatted(): String {
    val now = Date().time
    var pattern = "d LLL yyyy, HH:mm"
    if (isSameDay(now)) {
        pattern = "HH:mm"
    } else if (isSameYear(now)) {
        pattern = "d LLL, HH:mm"
    }
    val formatter = DateTimeFormatter.ofPattern(pattern)
    val formatted = formatter.format(Instant.ofEpochMilli(this.time).atZone(ZoneId.systemDefault()).toLocalDateTime())
    if (isSameDay(now)) {
        return "Today, $formatted"
    }
    return formatted
}