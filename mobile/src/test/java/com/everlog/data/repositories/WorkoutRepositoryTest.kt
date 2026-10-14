package com.everlog.data.repositories

import com.everlog.data.model.exercise.ELExercise
import com.everlog.testutil.loggedSet
import com.everlog.testutil.workout
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutRepositoryTest {

    private val bench = ELExercise(uuid = "bench")
    private val row = ELExercise(uuid = "row")

    @Test
    fun `an exercise was last done in its newest workout, whatever the order`() {
        val older = workout(0L, bench to listOf(loggedSet(5, 100f)), row to listOf(loggedSet(8, 60f))).apply { createdDate = 100L }
        val newer = workout(0L, bench to listOf(loggedSet(5, 100f))).apply { createdDate = 200L }

        assertThat(exerciseLastDone(listOf(older, newer))).containsExactly("bench", 200L, "row", 100L)
        assertThat(exerciseLastDone(listOf(newer, older))).containsExactly("bench", 200L, "row", 100L)
    }
}
