package com.everlog.android.exercise.page

import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.workout.data.exercisehistory.ExerciseSession
import com.everlog.android.workout.data.exercisehistory.ExerciseStatsRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.threeten.bp.LocalDate

class ExercisePageRulesTest {

    private val today = LocalDate.of(2026, 11, 5)

    @Test
    fun `each session keeps its key as older ones go in above, a workout with the exercise twice having two`() {
        val shown = listOf(session("b"), session("c"), session("c"))
        val withOlder = listOf(session("a")) + shown

        assertThat(sessionKeys(shown)).containsExactly("b#1", "c#1", "c#2").inOrder()
        assertThat(sessionKeys(withOlder).drop(1)).isEqualTo(sessionKeys(shown))
    }

    @Test
    fun `days sit in order on the axis, labelled with the year only when it isn't this one`() {
        val week = ExerciseStatsRange.Week

        assertThat(week.chartX(today)).isGreaterThan(week.chartX(today.minusDays(1)))
        assertThat(week.chartLabel(week.chartX(today), today)).isEqualTo("5\nNov")
        assertThat(week.chartLabel(week.chartX(today.minusYears(1)), today)).isEqualTo("5\nNov, 2025")
    }

    @Test
    fun `the 12 months sit a point a month across the new year`() {
        val year = ExerciseStatsRange.Last12Months
        val december = LocalDate.of(2025, 12, 1)
        val january = LocalDate.of(2026, 1, 1)

        assertThat(year.chartX(january) - year.chartX(december)).isEqualTo(1f)
        assertThat(year.chartLabel(year.chartX(december), today)).isEqualTo("Dec, 2025")
        assertThat(year.chartLabel(year.chartX(january), today)).isEqualTo("Jan")
    }

    private fun session(workoutUuid: String) =
        ExerciseSession(workoutUuid, "Push", completedDate = 0, exercise = ELRoutineExercise("bench", ELExercise(uuid = "bench"), mutableListOf()))
}
