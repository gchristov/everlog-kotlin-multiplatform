package com.everlog.android.home.data.week

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.WeightUnit
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.TemporalAdjusters

// What the Week tab shows of the user's current week
data class WeekStats(
    // The week's seven days, from the user's first day of the week
    val days: List<LocalDate>,
    val workoutDays: Set<LocalDate>,
    val workouts: Int,
    val weightLifted: Float,
    val totalMillis: Long,
    val averageMillis: Long,
)

// The week [today] is in, of the [workouts] finished in it
fun weekStats(
    workouts: List<ELWorkout>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    weightUnit: WeightUnit,
    zone: ZoneId = ZoneId.systemDefault(),
): WeekStats {
    val start = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val days = (0L until DaysPerWeek).map { start.plusDays(it) }
    val inWeek = workouts.filter { it.completedDate > 0 && it.completedOn(zone) in days }
    val totalMillis = inWeek.sumOf { it.getDurationMillis() }
    return WeekStats(
        days = days,
        workoutDays = inWeek.map { it.completedOn(zone) }.toSet(),
        workouts = inWeek.size,
        weightLifted = inWeek.sumOf { it.getTotalWeight(weightUnit).toDouble() }.toFloat(),
        totalMillis = totalMillis,
        averageMillis = if (inWeek.isEmpty()) 0 else totalMillis / inWeek.size,
    )
}

private fun ELWorkout.completedOn(zone: ZoneId): LocalDate = Instant.ofEpochMilli(completedDate).atZone(zone).toLocalDate()

private const val DaysPerWeek = 7L
