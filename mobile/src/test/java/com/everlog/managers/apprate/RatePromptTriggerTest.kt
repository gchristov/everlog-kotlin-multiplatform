package com.everlog.managers.apprate

import com.everlog.data.model.workout.ELWorkout
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit

class RatePromptTriggerTest {

    private fun workoutLasting(minutes: Long, seconds: Long = 0, uuid: String? = "abc"): ELWorkout {
        val createdDate = 1_000_000L
        return ELWorkout(uuid = uuid, createdDate = createdDate, completedDate = createdDate + TimeUnit.MINUTES.toMillis(minutes) + TimeUnit.SECONDS.toMillis(seconds))
    }

    @Test
    fun `a 5 minute workout qualifies`() {
        assertThat(RatePromptTrigger.WorkoutCompleted(workoutLasting(minutes = 5)).isEligible()).isTrue()
    }

    @Test
    fun `a long workout qualifies`() {
        assertThat(RatePromptTrigger.WorkoutCompleted(workoutLasting(minutes = 75)).isEligible()).isTrue()
    }

    @Test
    fun `a workout just under 5 minutes doesn't qualify`() {
        assertThat(RatePromptTrigger.WorkoutCompleted(workoutLasting(minutes = 4, seconds = 59)).isEligible()).isFalse()
    }

    @Test
    fun `logs the workout source`() {
        assertThat(RatePromptTrigger.WorkoutCompleted(workoutLasting(minutes = 5)).source).isEqualTo("workout_completed")
    }

    @Test
    fun `identifies the action by workout`() {
        assertThat(RatePromptTrigger.WorkoutCompleted(workoutLasting(minutes = 5, uuid = "abc")).actionId)
            .isEqualTo(RatePromptTrigger.WorkoutCompleted(workoutLasting(minutes = 5, uuid = "abc")).actionId)
        assertThat(RatePromptTrigger.WorkoutCompleted(workoutLasting(minutes = 5, uuid = "abc")).actionId)
            .isNotEqualTo(RatePromptTrigger.WorkoutCompleted(workoutLasting(minutes = 5, uuid = "def")).actionId)
    }

    @Test
    fun `identifies a workout without an id by its completed date`() {
        assertThat(RatePromptTrigger.WorkoutCompleted(workoutLasting(minutes = 5, uuid = null)).actionId)
            .isNotEqualTo(RatePromptTrigger.WorkoutCompleted(workoutLasting(minutes = 6, uuid = null)).actionId)
    }
}
