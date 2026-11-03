package com.everlog.android.template.data.store

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.exercise.ELRoutineExercise
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.exercise.data.ExerciseLookup
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ELRoutineDecoratorTest {

    private val renamed = ELExercise(uuid = "bench", name = "Barbell bench press", imageUrl = "new.png")
    private val lookup = object : ExerciseLookup {
        val asked = mutableListOf<Set<String>>()

        override fun find(uuids: Set<String>): Map<String, ELExercise> {
            asked += uuids
            return mapOf("bench" to renamed).filterKeys { it in uuids }
        }
    }
    private val decorator = ELRoutineDecorator(lookup)

    @Test
    fun `a routine shows its exercises as they are now`() {
        val routine = routineWith("bench", "row")

        decorator.decorate(routine)

        assertThat(lookup.asked).containsExactly(setOf("bench", "row"))
        assertThat(routine.exercise("bench")).isSameInstanceAs(renamed)
    }

    @Test
    fun `an exercise the lookup doesn't have keeps the copy saved with the workout`() {
        val workout = ELWorkout(routine = routineWith("bench", "row"))
        val saved = workout.routine!!.exercise("row")

        decorator.decorate(workout)

        assertThat(workout.routine!!.exercise("bench")).isSameInstanceAs(renamed)
        assertThat(workout.routine!!.exercise("row")).isSameInstanceAs(saved)
    }

    private fun routineWith(vararg uuids: String) = ELRoutine(
        exerciseGroups = uuids.map { uuid ->
            ELExerciseGroup(exercises = mutableListOf(ELRoutineExercise(uuid, ELExercise(uuid = uuid, name = "Saved $uuid"))))
        }.toMutableList()
    )

    private fun ELRoutine.exercise(uuid: String) = exerciseGroups.flatMap { it.exercises }.first { it.uuid == uuid }.exercise
}
