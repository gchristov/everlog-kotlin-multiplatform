package com.everlog.android.home.data.week

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.datatest.workout
import com.everlog.android.common.kotlin.WeightUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import java.util.concurrent.TimeUnit

class WeekStatsTest {

    private val zone = ZoneOffset.UTC
    private val bench = ELExercise(uuid = "bench")

    // Wednesday
    private val today = LocalDate.of(2026, 11, 4)

    private fun finished(day: LocalDate, minutes: Long = 60, weightKg: Float = 100f): ELWorkout {
        val completed = LocalDateTime.of(day.year, day.monthValue, day.dayOfMonth, 18, 0).toInstant(zone).toEpochMilli()
        return workout(completed, bench to listOf(loggedSet(5, weightKg))).apply {
            createdDate = completed - TimeUnit.MINUTES.toMillis(minutes)
        }
    }

    private fun stats(vararg workouts: ELWorkout, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY) =
        weekStats(workouts.toList(), today, firstDayOfWeek, WeightUnit.KILOGRAM, zone)

    @Test
    fun `the week runs from the user's first day of the week`() {
        assertThat(stats().days.first()).isEqualTo(LocalDate.of(2026, 11, 2))
        assertThat(stats(firstDayOfWeek = DayOfWeek.SUNDAY).days.first()).isEqualTo(LocalDate.of(2026, 11, 1))
        assertThat(stats(firstDayOfWeek = DayOfWeek.SATURDAY).days.first()).isEqualTo(LocalDate.of(2026, 10, 31))
        assertThat(stats().days).hasSize(7)
    }

    @Test
    fun `counts only the workouts finished this week`() {
        val stats = stats(
            finished(LocalDate.of(2026, 11, 2), minutes = 40, weightKg = 100f),
            finished(LocalDate.of(2026, 11, 4), minutes = 80, weightKg = 50f),
            finished(LocalDate.of(2026, 11, 1)),
            finished(LocalDate.of(2026, 11, 9)),
        )

        assertThat(stats.workouts).isEqualTo(2)
        assertThat(stats.weightLifted).isEqualTo(750f)
        assertThat(stats.totalMillis).isEqualTo(TimeUnit.MINUTES.toMillis(120))
        assertThat(stats.averageMillis).isEqualTo(TimeUnit.MINUTES.toMillis(60))
        assertThat(stats.workoutDays).containsExactly(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4))
    }

    @Test
    fun `a week across the new year keeps its days in both years`() {
        val newYear = weekStats(
            listOf(finished(LocalDate.of(2026, 12, 30)), finished(LocalDate.of(2027, 1, 2))),
            LocalDate.of(2027, 1, 1),
            DayOfWeek.MONDAY,
            WeightUnit.KILOGRAM,
            zone,
        )

        assertThat(newYear.workouts).isEqualTo(2)
    }

    @Test
    fun `an empty week has nothing to average`() {
        val stats = stats(finished(LocalDate.of(2026, 10, 20)))

        assertThat(stats.workouts).isEqualTo(0)
        assertThat(stats.averageMillis).isEqualTo(0)
        assertThat(stats.workoutDays).isEmpty()
    }

    @Test
    fun `an unfinished workout doesn't count`() {
        val ongoing = finished(today).apply { completedDate = 0 }

        assertThat(stats(ongoing).workouts).isEqualTo(0)
    }
}
