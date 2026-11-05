package com.everlog.android.statistics.ui.page

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.statistics.data.StatsPeriod
import com.everlog.android.statistics.data.UserStats
import com.everlog.android.statistics.data.userStats
import com.everlog.android.workout.data.example.exampleWorkouts
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

// An empty period's example: the sample workouts over the whole week or month [today] is in, so it shows what a full
// one looks like, or the 12 months up to today
internal fun exampleStats(period: StatsPeriod, today: LocalDate, firstDayOfWeek: DayOfWeek, weightUnit: WeightUnit): UserStats {
    val until = if (period == StatsPeriod.Last12Months) today else userStats(emptyList(), period, today, firstDayOfWeek, weightUnit).until.minusDays(1)
    return userStats(exampleWorkouts(until), period, until, firstDayOfWeek, weightUnit)
}
