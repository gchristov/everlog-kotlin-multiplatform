package com.everlog.android.workout.data.example

import com.everlog.android.common.kotlin.WeightUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

class ExampleWorkoutsTest {

    private val zone = ZoneId.of("Europe/London")
    private val until = LocalDate.of(2026, 11, 5)

    @Test
    fun `three a week for the year up to the day, taking turns through the names`() {
        val workouts = exampleWorkouts(until, listOf("Legs", "Push", "Pull"), zone)
        val days = workouts.map { Instant.ofEpochMilli(it.completedDate).atZone(zone).toLocalDate() }

        assertThat(days.map { it.dayOfWeek }.toSet()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertThat(days.first()).isAtLeast(until.minusYears(1))
        assertThat(days.last()).isAtMost(until)
        assertThat(workouts.take(4).map { it.getName() }).containsExactly("Legs", "Push", "Pull", "Legs").inOrder()
    }

    @Test
    fun `each has a duration, weights and muscles, which go up over the year`() {
        val workouts = exampleWorkouts(until, zone = zone)

        assertThat(workouts.all { it.getDurationMillis() > 0 }).isTrue()
        assertThat(workouts.flatMap { it.routine!!.exerciseGroups }.flatMap { it.exercises }.mapNotNull { it.exercise?.category }.toSet())
            .containsExactly("LEGS", "ABS", "CHEST", "SHOULDERS", "ARMS", "BACK")
        val legDays = workouts.filterIndexed { index, _ -> index % 3 == 0 }
        assertThat(legDays.last().getMaxWeight(WeightUnit.KILOGRAM))
            .isGreaterThan(legDays.first().getMaxWeight(WeightUnit.KILOGRAM))
    }

    @Test
    fun `the same every time`() {
        assertThat(exampleWorkouts(until, zone = zone)).isEqualTo(exampleWorkouts(until, zone = zone))
    }
}
