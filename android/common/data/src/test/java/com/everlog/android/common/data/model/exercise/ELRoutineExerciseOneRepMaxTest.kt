package com.everlog.android.common.data.model.exercise

import com.everlog.android.common.data.model.set.ELSet
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.kotlin.WeightUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ELRoutineExerciseOneRepMaxTest {

    private val bench = ELExercise(uuid = "bench", name = "Bench press")

    @Test
    fun `1RM is estimated from the heaviest set`() {
        val exercise = routineExercise(loggedSet(8, 60f), loggedSet(5, 70f), loggedSet(8, 65f))

        // 70 / (1.0278 - 0.0278 * 5)
        assertThat(exercise.estimated1RM(WeightUnit.KILOGRAM)).isWithin(0.01f).of(78.76f)
    }

    @Test
    fun `1RM ignores sets with more than 10 reps`() {
        // 80kg x 12 would give a 1RM of 115
        val exercise = routineExercise(loggedSet(12, 80f), loggedSet(5, 70f))

        assertThat(exercise.estimated1RM(WeightUnit.KILOGRAM)).isWithin(0.01f).of(78.76f)
    }

    @Test
    fun `1RM counts sets with exactly 10 reps`() {
        val exercise = routineExercise(loggedSet(10, 80f))

        // 80 / (1.0278 - 0.0278 * 10)
        assertThat(exercise.estimated1RM(WeightUnit.KILOGRAM)).isWithin(0.01f).of(106.70f)
    }

    @Test
    fun `1RM is 0 without a set to estimate it from`() {
        val exercise = routineExercise(loggedSet(30, 20f), ELSet(reps = 5), ELSet(weight = 60f))

        assertThat(exercise.estimated1RM(WeightUnit.KILOGRAM)).isEqualTo(0f)
    }

    private fun routineExercise(vararg sets: ELSet) = ELRoutineExercise("re", bench, sets.toMutableList())
}
