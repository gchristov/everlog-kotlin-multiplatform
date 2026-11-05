package com.everlog.android.statistics.data

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.WeightUnit
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import kotlin.math.max

// The stretches the statistics cover
enum class StatsPeriod {
    Week,
    Month,
    // The current month and the 11 before it
    Last12Months,
}

/**
 * The user's statistics over a period, from its first day until before [until].
 *
 * @param muscles How many exercises the period's workouts had of each category, by the category's id
 * @param weightLifted The weight lifted in each day (for a week or month) or month (for the 12 months) with a
 * workout, oldest first
 * @param duration The average workout length in minutes in each such day or month, oldest first
 */
data class UserStats(
    val period: StatsPeriod,
    val from: LocalDate,
    val until: LocalDate,
    val workouts: Int,
    val totalWeight: Float,
    val totalMillis: Long,
    val averageMillis: Long,
    val longestMillis: Long,
    val maxWeight: Float,
    val muscles: Map<String, Int>,
    val weightLifted: List<StatsPoint>,
    val duration: List<StatsPoint>,
)

// A day's or month's value, by the day or month's first day
data class StatsPoint(val start: LocalDate, val value: Float)

fun userStats(
    workouts: List<ELWorkout>,
    period: StatsPeriod,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    weightUnit: WeightUnit,
    zone: ZoneId = ZoneId.systemDefault(),
): UserStats {
    val from = period.start(today, firstDayOfWeek)
    val until = period.end(from)
    val inPeriod = workouts
        .filter { it.completedDate > 0 }
        .map { it to it.completedOn(zone) }
        .filter { (_, day) -> !day.isBefore(from) && day.isBefore(until) }
        .sortedBy { (_, day) -> day }
    val totalMillis = inPeriod.sumOf { (workout, _) -> workout.getDurationMillis() }
    val buckets = inPeriod.groupBy { (_, day) -> period.bucketOf(day) }
    return UserStats(
        period = period,
        from = from,
        until = until,
        workouts = inPeriod.size,
        totalWeight = inPeriod.sumOf { (workout, _) -> workout.getTotalWeight(weightUnit).toDouble() }.toFloat(),
        totalMillis = totalMillis,
        averageMillis = if (inPeriod.isEmpty()) 0 else totalMillis / inPeriod.size,
        longestMillis = inPeriod.maxOfOrNull { (workout, _) -> workout.getDurationMillis() } ?: 0,
        maxWeight = inPeriod.fold(0f) { best, (workout, _) -> max(best, workout.getMaxWeight(weightUnit)) },
        muscles = muscleCounts(inPeriod.map { (workout, _) -> workout }),
        weightLifted = buckets.map { (start, inBucket) ->
            StatsPoint(start, inBucket.sumOf { (workout, _) -> workout.getTotalWeight(weightUnit).toDouble() }.toFloat())
        },
        duration = buckets.map { (start, inBucket) ->
            StatsPoint(start, inBucket.map { (workout, _) -> TimeUnit.MILLISECONDS.toMinutes(workout.getDurationMillis()).toFloat() }.average().toFloat())
        },
    )
}

// Exercises without a category, e.g. one whose exercise has gone, don't count, rather than fail the whole period as
// ELWorkout.getCategoryCounts does
private fun muscleCounts(workouts: List<ELWorkout>): Map<String, Int> = buildMap {
    workouts.flatMap { it.getExerciseGroups() }.flatMap { it.exercises }.forEach { exercise ->
        exercise.exercise?.category?.let { put(it, (get(it) ?: 0) + 1) }
    }
}

private fun StatsPeriod.start(today: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate = when (this) {
    StatsPeriod.Week -> today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    StatsPeriod.Month -> today.withDayOfMonth(1)
    StatsPeriod.Last12Months -> today.withDayOfMonth(1).minusMonths(11)
}

private fun StatsPeriod.end(from: LocalDate): LocalDate = when (this) {
    StatsPeriod.Week -> from.plusWeeks(1)
    StatsPeriod.Month -> from.plusMonths(1)
    StatsPeriod.Last12Months -> from.plusMonths(12)
}

private fun StatsPeriod.bucketOf(day: LocalDate): LocalDate = when (this) {
    StatsPeriod.Week, StatsPeriod.Month -> day
    StatsPeriod.Last12Months -> day.withDayOfMonth(1)
}

private fun ELWorkout.completedOn(zone: ZoneId): LocalDate = Instant.ofEpochMilli(completedDate).atZone(zone).toLocalDate()
