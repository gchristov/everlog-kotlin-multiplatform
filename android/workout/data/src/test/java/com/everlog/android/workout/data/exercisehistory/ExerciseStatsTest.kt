package com.everlog.android.workout.data.exercisehistory

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.kotlin.WeightUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

class ExerciseStatsTest {

    private val zone = ZoneId.of("UTC")
    // A Thursday
    private val today = LocalDate.of(2026, 11, 5)
    private val squat = ELExercise(uuid = "squat", name = "Squat")

    // Main journeys

    @Test
    fun `this week's totals, heaviest set and best 1RM`() {
        val sessions = listOf(
            session("a", today, loggedSet(5, 100f), loggedSet(5, 110f)),
            session("b", today.minusDays(2), loggedSet(3, 120f)),
            // Last week
            session("c", today.minusDays(7), loggedSet(1, 200f)),
        )

        val stats = stats(sessions, ExerciseStatsRange.Week)

        assertThat(stats.from).isEqualTo(LocalDate.of(2026, 11, 2))
        assertThat(stats.until).isEqualTo(LocalDate.of(2026, 11, 9))
        assertThat(stats.timesPerformed).isEqualTo(2)
        assertThat(stats.totalReps).isEqualTo(13)
        assertThat(stats.totalWeight).isEqualTo(5 * 100f + 5 * 110f + 3 * 120f)
        assertThat(stats.heaviestSet?.weight).isEqualTo(120f)
        assertThat(stats.best1RM).isGreaterThan(120f)
    }

    @Test
    fun `a week, month or today has a point per day it was done, oldest first`() {
        val sessions = listOf(
            session("a", today, loggedSet(5, 100f)),
            session("b", today.minusDays(2), loggedSet(8, 80f)),
        )

        val stats = stats(sessions, ExerciseStatsRange.Month)

        assertThat(stats.reps).containsExactly(
            ExerciseStatsPoint(today.minusDays(2), 8f),
            ExerciseStatsPoint(today, 5f),
        ).inOrder()
        assertThat(stats.maxWeight.map { it.value }).containsExactly(80f, 100f).inOrder()
        assertThat(stats.oneRepMax.map { it.start }).containsExactly(today.minusDays(2), today).inOrder()
    }

    @Test
    fun `the last 12 months have a point per month, from 11 months back`() {
        val sessions = listOf(
            session("a", today, loggedSet(5, 100f)),
            session("b", today.minusDays(1), loggedSet(5, 105f)),
            session("c", LocalDate.of(2025, 12, 20), loggedSet(5, 90f)),
            // Before the 12 months
            session("d", LocalDate.of(2025, 11, 30), loggedSet(5, 80f)),
        )

        val stats = stats(sessions, ExerciseStatsRange.Last12Months)

        assertThat(stats.from).isEqualTo(LocalDate.of(2025, 12, 1))
        assertThat(stats.timesPerformed).isEqualTo(3)
        assertThat(stats.reps).containsExactly(
            ExerciseStatsPoint(LocalDate.of(2025, 12, 1), 5f),
            ExerciseStatsPoint(LocalDate.of(2026, 11, 1), 10f),
        ).inOrder()
        assertThat(stats.maxWeight.last().value).isEqualTo(105f)
    }

    @Test
    fun `today counts only today`() {
        val sessions = listOf(session("a", today, loggedSet(5, 100f)), session("b", today.minusDays(1), loggedSet(5, 100f)))

        val stats = stats(sessions, ExerciseStatsRange.Today)

        assertThat(stats.timesPerformed).isEqualTo(1)
        assertThat(stats.until).isEqualTo(today.plusDays(1))
    }

    // Medium importance

    @Test
    fun `an exercise twice in one workout counts the workout once and both sessions' sets`() {
        val sessions = listOf(session("a", today, loggedSet(5, 100f)), session("a", today, loggedSet(5, 100f)))

        val stats = stats(sessions, ExerciseStatsRange.Week)

        assertThat(stats.timesPerformed).isEqualTo(1)
        assertThat(stats.totalReps).isEqualTo(10)
    }

    @Test
    fun `weights are in the user's unit`() {
        val stats = stats(listOf(session("a", today, loggedSet(5, 100f))), ExerciseStatsRange.Week, WeightUnit.POUND)

        assertThat(stats.totalWeight).isWithin(0.5f).of(5 * 220.5f)
        assertThat(stats.maxWeight.single().value).isWithin(0.5f).of(220.5f)
    }

    @Test
    fun `the week starts on the user's first day`() {
        val stats = stats(emptyList(), ExerciseStatsRange.Week, firstDayOfWeek = DayOfWeek.SUNDAY)

        assertThat(stats.from).isEqualTo(LocalDate.of(2026, 11, 1))
    }

    // Edge cases

    @Test
    fun `without sessions in the range everything is empty`() {
        val stats = stats(listOf(session("a", today.minusMonths(2), loggedSet(5, 100f))), ExerciseStatsRange.Month)

        assertThat(stats.timesPerformed).isEqualTo(0)
        assertThat(stats.totalReps).isEqualTo(0)
        assertThat(stats.heaviestSet).isNull()
        assertThat(stats.best1RM).isEqualTo(0f)
        assertThat(stats.reps).isEmpty()
        assertThat(stats.oneRepMax).isEmpty()
    }

    @Test
    fun `bodyweight sets have reps but no heaviest set, weight or 1RM`() {
        val stats = stats(listOf(session("a", today, ELSet(reps = 12))), ExerciseStatsRange.Week)

        assertThat(stats.totalReps).isEqualTo(12)
        assertThat(stats.heaviestSet).isNull()
        assertThat(stats.maxWeight.single().value).isEqualTo(0f)
        assertThat(stats.oneRepMax).isEmpty()
    }

    private fun stats(
        sessions: List<ExerciseSession>,
        range: ExerciseStatsRange,
        unit: WeightUnit = WeightUnit.KILOGRAM,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ) = exerciseStats(sessions, range, today, firstDayOfWeek, unit, zone)

    private fun session(workoutUuid: String, day: LocalDate, vararg sets: ELSet) = ExerciseSession(
        workoutUuid = workoutUuid,
        workoutName = "Legs",
        completedDate = day.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
        exercise = ELRoutineExercise(squat.uuid, squat, sets.toMutableList()),
    )
}
