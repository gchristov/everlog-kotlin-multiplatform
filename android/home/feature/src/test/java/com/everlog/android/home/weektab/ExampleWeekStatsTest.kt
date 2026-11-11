package com.everlog.android.home.weektab

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.home.data.week.WeekStats
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.threeten.bp.LocalDate

class ExampleWeekStatsTest {

    private val week = WeekStats(
        days = (0L until 7).map { LocalDate.of(2026, 11, 9).plusDays(it) },
        workoutDays = emptySet(),
        workouts = 0,
        weightLifted = 0f,
        totalMillis = 0,
        averageMillis = 0,
    )

    @Test
    fun `one workout short of the goal, each about an hour`() {
        val example = exampleWeekStats(week, weeklyGoal = 3, unit = WeightUnit.KILOGRAM)

        assertThat(example.workouts).isEqualTo(2)
        assertThat(example.weightLifted).isEqualTo(6_300f)
        assertThat(example.totalMillis).isEqualTo(130 * 60_000L)
        assertThat(example.averageMillis).isEqualTo(65 * 60_000L)
        assertThat(example.days).isEqualTo(week.days)
    }

    @Test
    fun `its days every other day from the week's start, then the days between`() {
        assertThat(exampleWeekStats(week, weeklyGoal = 3, unit = WeightUnit.KILOGRAM).workoutDays)
            .containsExactly(week.days[0], week.days[2])
        assertThat(exampleWeekStats(week, weeklyGoal = 7, unit = WeightUnit.KILOGRAM).workoutDays)
            .containsExactly(week.days[0], week.days[2], week.days[4], week.days[6], week.days[1], week.days[3])
    }

    @Test
    fun `at least one workout, for a goal of one`() {
        assertThat(exampleWeekStats(week, weeklyGoal = 1, unit = WeightUnit.KILOGRAM).workouts).isEqualTo(1)
    }

    @Test
    fun `the weight in pounds for a pounds user`() {
        assertThat(exampleWeekStats(week, weeklyGoal = 4, unit = WeightUnit.POUND).weightLifted).isEqualTo(3 * 6_945f)
    }
}
