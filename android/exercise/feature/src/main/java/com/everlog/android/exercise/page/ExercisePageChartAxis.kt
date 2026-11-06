package com.everlog.android.exercise.page

import com.everlog.android.workout.data.exercisehistory.ExerciseStatsRange
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

// Where a day's or month's point sits on a chart's x axis: days for today, a week or a month, months for the 12
// months, both counted from 1970, so they keep their order across the new year
internal fun ExerciseStatsRange.chartX(start: LocalDate): Float = when (this) {
    ExerciseStatsRange.Last12Months -> (start.year * MonthsPerYear + start.monthValue - 1).toFloat()
    else -> start.toEpochDay().toFloat()
}

// The label under a point, with the year only when it isn't this one: "4\nNov" for a day, "Dec, 2025" for a month
internal fun ExerciseStatsRange.chartLabel(x: Float, today: LocalDate): String {
    val date = when (this) {
        ExerciseStatsRange.Last12Months -> x.toInt().let { LocalDate.of(it / MonthsPerYear, it % MonthsPerYear + 1, 1) }
        else -> LocalDate.ofEpochDay(x.toLong())
    }
    val year = if (date.year == today.year) "" else ", yyyy"
    val pattern = when (this) {
        ExerciseStatsRange.Last12Months -> "LLL$year"
        else -> "d\nLLL$year"
    }
    return DateTimeFormatter.ofPattern(pattern).format(date)
}

private const val MonthsPerYear = 12
