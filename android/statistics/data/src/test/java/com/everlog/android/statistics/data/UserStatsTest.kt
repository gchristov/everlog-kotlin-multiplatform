package com.everlog.android.statistics.data

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.datatest.workout
import com.everlog.android.common.kotlin.WeightUnit
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset

class UserStatsTest {

    private val zone = ZoneOffset.UTC
    private val bench = ELExercise(uuid = "bench", category = "CHEST")
    private val squat = ELExercise(uuid = "squat", category = "LEGS")

    // Wednesday
    private val today = LocalDate.of(2026, 11, 4)

    private fun finished(day: LocalDate, minutes: Long = 60, benchKg: Float = 100f, withSquat: Boolean = false): ELWorkout {
        val completed = day.atTime(18, 0).toInstant(zone).toEpochMilli()
        val exercises = listOfNotNull(bench to listOf(loggedSet(5, benchKg)), if (withSquat) squat to listOf(loggedSet(5, 140f)) else null)
        return workout(completed, *exercises.toTypedArray()).apply { createdDate = completed - TimeUnit.MINUTES.toMillis(minutes) }
    }

    private fun stats(period: StatsPeriod, vararg workouts: ELWorkout, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY) =
        userStats(workouts.toList(), period, today, firstDayOfWeek, WeightUnit.KILOGRAM, zone)

    @Test
    fun `each period runs from its first day`() {
        assertThat(stats(StatsPeriod.Week).from).isEqualTo(LocalDate.of(2026, 11, 2))
        assertThat(stats(StatsPeriod.Week).until).isEqualTo(LocalDate.of(2026, 11, 9))
        assertThat(stats(StatsPeriod.Week, firstDayOfWeek = DayOfWeek.SUNDAY).from).isEqualTo(LocalDate.of(2026, 11, 1))
        assertThat(stats(StatsPeriod.Month).from).isEqualTo(LocalDate.of(2026, 11, 1))
        assertThat(stats(StatsPeriod.Month).until).isEqualTo(LocalDate.of(2026, 12, 1))
        assertThat(stats(StatsPeriod.Last12Months).from).isEqualTo(LocalDate.of(2025, 12, 1))
        assertThat(stats(StatsPeriod.Last12Months).until).isEqualTo(LocalDate.of(2026, 12, 1))
    }

    @Test
    fun `counts only the period's finished workouts`() {
        val stats = stats(
            StatsPeriod.Month,
            finished(LocalDate.of(2026, 11, 2), minutes = 40, benchKg = 100f),
            finished(LocalDate.of(2026, 11, 3), minutes = 80, benchKg = 60f, withSquat = true),
            finished(LocalDate.of(2026, 10, 31)),
            finished(today).apply { completedDate = 0 },
        )

        assertThat(stats.workouts).isEqualTo(2)
        assertThat(stats.totalWeight).isEqualTo(500f + 300f + 700f)
        assertThat(stats.totalMillis).isEqualTo(TimeUnit.MINUTES.toMillis(120))
        assertThat(stats.averageMillis).isEqualTo(TimeUnit.MINUTES.toMillis(60))
        assertThat(stats.longestMillis).isEqualTo(TimeUnit.MINUTES.toMillis(80))
        assertThat(stats.maxWeight).isEqualTo(140f)
        assertThat(stats.muscles).containsExactly("CHEST", 2, "LEGS", 1)
    }

    @Test
    fun `a week or month charts each day with a workout`() {
        val stats = stats(
            StatsPeriod.Week,
            finished(LocalDate.of(2026, 11, 3), minutes = 30, benchKg = 100f),
            finished(LocalDate.of(2026, 11, 2), minutes = 60, benchKg = 50f),
            finished(LocalDate.of(2026, 11, 3), minutes = 90, benchKg = 100f),
        )

        assertThat(stats.weightLifted).containsExactly(
            StatsPoint(LocalDate.of(2026, 11, 2), 250f),
            StatsPoint(LocalDate.of(2026, 11, 3), 1000f),
        ).inOrder()
        assertThat(stats.duration).containsExactly(
            StatsPoint(LocalDate.of(2026, 11, 2), 60f),
            StatsPoint(LocalDate.of(2026, 11, 3), 60f),
        ).inOrder()
    }

    @Test
    fun `the last 12 months chart each month, across the new year`() {
        val stats = stats(
            StatsPeriod.Last12Months,
            finished(LocalDate.of(2026, 1, 20), benchKg = 100f),
            finished(LocalDate.of(2025, 12, 5), benchKg = 100f),
            finished(LocalDate.of(2025, 12, 28), benchKg = 100f),
            finished(LocalDate.of(2025, 11, 30)),
        )

        assertThat(stats.workouts).isEqualTo(3)
        assertThat(stats.weightLifted).containsExactly(
            StatsPoint(LocalDate.of(2025, 12, 1), 1000f),
            StatsPoint(LocalDate.of(2026, 1, 1), 500f),
        ).inOrder()
    }

    @Test
    fun `an empty period has nothing to average or chart`() {
        val stats = stats(StatsPeriod.Week, finished(LocalDate.of(2026, 10, 1)))

        assertThat(stats.workouts).isEqualTo(0)
        assertThat(stats.averageMillis).isEqualTo(0)
        assertThat(stats.longestMillis).isEqualTo(0)
        assertThat(stats.muscles).isEmpty()
        assertThat(stats.weightLifted).isEmpty()
    }
}
