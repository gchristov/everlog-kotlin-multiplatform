package com.everlog.android.statistics.ui.page

import com.everlog.android.statistics.data.StatsPeriod
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

// Where a day's or month's point sits on a chart's x axis: days for a week or month, months for the 12 months, both
// counted from 1970, so they keep their order across the new year
fun StatsPeriod.chartX(start: LocalDate): Float = when (this) {
    StatsPeriod.Week, StatsPeriod.Month -> start.toEpochDay().toFloat()
    StatsPeriod.Last12Months -> (start.year * MonthsPerYear + start.monthValue - 1).toFloat()
}

// The label under a point, with the year only when it isn't this one: "4\nNov" for a day, "Dec, 2025" for a month
fun StatsPeriod.chartLabel(x: Float, today: LocalDate): String {
    val date = when (this) {
        StatsPeriod.Week, StatsPeriod.Month -> LocalDate.ofEpochDay(x.toLong())
        StatsPeriod.Last12Months -> x.toInt().let { LocalDate.of(it / MonthsPerYear, it % MonthsPerYear + 1, 1) }
    }
    val year = if (date.year == today.year) "" else ", yyyy"
    val pattern = when (this) {
        StatsPeriod.Week, StatsPeriod.Month -> "d\nLLL$year"
        StatsPeriod.Last12Months -> "LLL$year"
    }
    return DateTimeFormatter.ofPattern(pattern).format(date)
}

private const val MonthsPerYear = 12
