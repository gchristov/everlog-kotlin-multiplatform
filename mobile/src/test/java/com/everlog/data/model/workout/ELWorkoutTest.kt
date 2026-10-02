package com.everlog.data.model.workout

import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.set.ELSet
import com.everlog.testutil.InMemorySharedPreferences
import com.everlog.testutil.at
import com.everlog.testutil.workout
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class ELWorkoutTest {

    private val plank = ELExercise(uuid = "plank", name = "Plank")

    @Before
    fun setUp() {
        InMemorySharedPreferences.install()
    }

    @After
    fun tearDown() {
        InMemorySharedPreferences.uninstall()
    }

    @Test
    fun `starting a workout copies template targets and clears timer countdowns`() {
        val set = ELSet(requiredTimeSeconds = 60, remainingTimeSeconds = 20)
        val ongoing = workout(at(2026, 10, 1), plank to listOf(set))

        ongoing.prefillRequiredMetrics()

        assertThat(set.getTimeSeconds()).isEqualTo(60)
        assertThat(set.remainingTimeSeconds).isNull()
    }

    @Test
    fun `resuming a workout clears timer countdowns`() {
        // Saved while the set's timer was running, e.g. before the app was killed
        val set = ELSet(timeSeconds = 60, remainingTimeSeconds = 20)
        val ongoing = workout(at(2026, 10, 1), plank to listOf(set))

        ongoing.clearRemainingTimes()

        assertThat(set.remainingTimeSeconds).isNull()
        assertThat(set.getTimeSeconds()).isEqualTo(60)
    }

    @Test
    fun `resuming a workout doesn't bring back cleared template targets`() {
        // The user cleared the template's time on this set
        val set = ELSet(requiredTimeSeconds = 60)
        val ongoing = workout(at(2026, 10, 1), plank to listOf(set))

        ongoing.clearRemainingTimes()

        assertThat(set.isTimeEntered()).isFalse()
    }

    @Test
    fun `counts completed sets across exercises`() {
        val squat = ELExercise(uuid = "squat", name = "Squat")
        val done = ELSet(reps = 5).apply { updateCompletedDate(at(2026, 10, 1)) }
        val alsoDone = ELSet(reps = 5).apply { updateCompletedDate(at(2026, 10, 1)) }
        // Filled in but not ticked off
        val notDone = ELSet(reps = 5)
        val ongoing = workout(at(2026, 10, 1), plank to listOf(done, notDone), squat to listOf(alsoDone))

        assertThat(ongoing.getCompletedSetsCount()).isEqualTo(2)
    }

    @Test
    fun `counts no completed sets in a workout that's just started`() {
        val ongoing = workout(at(2026, 10, 1), plank to listOf(ELSet(requiredTimeSeconds = 60)))

        assertThat(ongoing.getCompletedSetsCount()).isEqualTo(0)
    }
}
