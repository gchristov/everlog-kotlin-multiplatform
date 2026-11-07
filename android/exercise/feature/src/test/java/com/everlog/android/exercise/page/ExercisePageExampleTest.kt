package com.everlog.android.exercise.page

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

class ExercisePageExampleTest {

    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 11, 7)
    private val names = listOf("Chest", "Upper body", "Full body")
    private val sessions = exampleSessions(today, names, zone)

    @Test
    fun `the same day gives the same example, so it doesn't change as the page redraws`() {
        assertThat(exampleSessions(today, names, zone)).isEqualTo(sessions)
        assertThat(exampleSessions(today.plusDays(1), names, zone)).isNotEqualTo(sessions)
    }

    @Test
    fun `a year of two or three sessions a week, today included`() {
        val days = sessions.map { Instant.ofEpochMilli(it.completedDate).atZone(zone).toLocalDate() }

        assertThat(days.first()).isEqualTo(today)
        assertThat(days.last()).isAtLeast(today.minusYears(1))
        assertThat(sessions.size).isIn(80..160)
        assertThat(sessions.map { it.workoutName }.toSet()).containsExactlyElementsIn(names)
    }

    @Test
    fun `sets look like real ones, a lighter warm-up then working sets in plates with reps that vary`() {
        sessions.forEach { session ->
            val sets = session.exercise.sets
            assertThat(sets.size).isIn(3..5)
            assertThat(sets.first().weight!!).isLessThan(sets[1].weight!!)
            sets.forEach { set ->
                assertThat(set.weight!! % 2.5f).isEqualTo(0f)
                assertThat(set.getReps()).isIn(4..12)
            }
        }
        assertThat(sessions.flatMap { it.exercise.sets }.map { it.getReps() }.toSet().size).isAtLeast(5)
    }

    @Test
    fun `the weight goes up over the year`() {
        val newest = sessions.first().exercise.sets[1].weight!!
        val oldest = sessions.last().exercise.sets[1].weight!!

        assertThat(newest).isGreaterThan(oldest)
    }
}
