package com.everlog.android.workout.data.exercisehistory

import com.everlog.android.common.data.model.exercise.estimated1RM
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.kotlin.WeightUnit
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.TemporalAdjusters

// The stretches an exercise's statistics cover
enum class ExerciseStatsRange {
    Today,
    Week,
    Month,
    // The current month and the 11 before it
    Last12Months,
}

/**
 * An exercise's statistics over a range, from its first day until before [until].
 *
 * @param timesPerformed The workouts that logged it, each once however often it was in the workout
 * @param heaviestSet The set with the most weight, null without weighted sets
 * @param best1RM The highest one-rep max estimated from a set, 0 without sets to estimate it from
 * @param reps The reps done in each day (for today, a week or a month) or month (for the 12 months) it was done,
 * oldest first
 * @param maxWeight The heaviest set's weight in each such day or month
 * @param oneRepMax The best estimated one-rep max in each such day or month, where there's a set to estimate it from
 */
data class ExerciseStats(
    val range: ExerciseStatsRange,
    val from: LocalDate,
    val until: LocalDate,
    val timesPerformed: Int,
    val totalReps: Int,
    val totalWeight: Float,
    val heaviestSet: ELSet?,
    val best1RM: Float,
    val reps: List<ExerciseStatsPoint>,
    val maxWeight: List<ExerciseStatsPoint>,
    val oneRepMax: List<ExerciseStatsPoint>,
)

// A day's or month's value, by the day or month's first day
data class ExerciseStatsPoint(val start: LocalDate, val value: Float)

fun exerciseStats(
    sessions: List<ExerciseSession>,
    range: ExerciseStatsRange,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    weightUnit: WeightUnit,
    zone: ZoneId = ZoneId.systemDefault(),
): ExerciseStats {
    val from = range.start(today, firstDayOfWeek)
    val until = range.end(from)
    val inRange = sessions
        .map { it to Instant.ofEpochMilli(it.completedDate).atZone(zone).toLocalDate() }
        .filter { (_, day) -> !day.isBefore(from) && day.isBefore(until) }
        .sortedBy { (_, day) -> day }
    val heaviest = inRange.map { (session, _) -> session.heaviestSet() }.filterNotNull().fold(null as ELSet?) { best, set ->
        if (best == null || set.isBetterThan(best, true)) set else best
    }
    val buckets = inRange.groupBy({ (_, day) -> range.bucketOf(day) }, { (session, _) -> session })
    return ExerciseStats(
        range = range,
        from = from,
        until = until,
        timesPerformed = inRange.map { (session, _) -> session.workoutUuid }.distinct().size,
        totalReps = inRange.sumOf { (session, _) -> session.exercise.getTotalReps() },
        totalWeight = inRange.sumOf { (session, _) -> session.exercise.getTotalWeight(weightUnit).toDouble() }.toFloat(),
        heaviestSet = heaviest,
        best1RM = inRange.maxOfOrNull { (session, _) -> session.exercise.estimated1RM(weightUnit) } ?: 0f,
        reps = buckets.map { (start, inBucket) -> ExerciseStatsPoint(start, inBucket.sumOf { it.exercise.getTotalReps() }.toFloat()) },
        maxWeight = buckets.map { (start, inBucket) ->
            ExerciseStatsPoint(start, inBucket.maxOf { session -> session.heaviestSet()?.getWeight(weightUnit) ?: 0f })
        },
        oneRepMax = buckets.mapNotNull { (start, inBucket) ->
            inBucket.maxOf { it.exercise.estimated1RM(weightUnit) }.takeIf { it > 0 }?.let { ExerciseStatsPoint(start, it) }
        },
    )
}

// Sets without weight, e.g. bodyweight, aren't the heaviest
private fun ExerciseSession.heaviestSet(): ELSet? = exercise.getBestSet(true).takeIf { it.isWeightEntered() }

private fun ExerciseStatsRange.start(today: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate = when (this) {
    ExerciseStatsRange.Today -> today
    ExerciseStatsRange.Week -> today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    ExerciseStatsRange.Month -> today.withDayOfMonth(1)
    ExerciseStatsRange.Last12Months -> today.withDayOfMonth(1).minusMonths(11)
}

private fun ExerciseStatsRange.end(from: LocalDate): LocalDate = when (this) {
    ExerciseStatsRange.Today -> from.plusDays(1)
    ExerciseStatsRange.Week -> from.plusWeeks(1)
    ExerciseStatsRange.Month -> from.plusMonths(1)
    ExerciseStatsRange.Last12Months -> from.plusMonths(12)
}

private fun ExerciseStatsRange.bucketOf(day: LocalDate): LocalDate = when (this) {
    ExerciseStatsRange.Today, ExerciseStatsRange.Week, ExerciseStatsRange.Month -> day
    ExerciseStatsRange.Last12Months -> day.withDayOfMonth(1)
}
