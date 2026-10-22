package com.everlog.data.controllers.workoutsession

import com.everlog.data.controllers.workoutprefill.BaseWorkoutPrefillController.PrefillSource
import com.everlog.data.model.exercise.ELExercise
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkoutSessionPrefillTest {

    private val bench = WorkoutExercise(uuid = "bench", exercise = ELExercise(uuid = "bench", name = "Bench press"))
    private val sources = mapOf("bench" to PrefillSource(lastSession = null, orm = 0f))

    @Test
    fun `suggested values are filled in again from the new setting`() {
        val block = block(WorkoutSet(reps = 8, weightKg = 60f, suggestedReps = 8, suggestedKg = 60f))

        val refilled = block.withSuggestionsRefilled(sources, Prefill(weightKg = 72f, reps = 8)).set()

        assertThat(refilled.weightKg).isEqualTo(72f)
        assertThat(refilled.suggestedKg).isEqualTo(72f)
        assertThat(refilled.isSuggested(ValueField.Weight)).isTrue()
        assertThat(refilled.reps).isEqualTo(8)
    }

    @Test
    fun `values typed over stay as they are`() {
        val block = block(WorkoutSet(reps = 10, weightKg = 65f, suggestedReps = 8, suggestedKg = 60f))

        val refilled = block.withSuggestionsRefilled(sources, Prefill(weightKg = 72f, reps = 6)).set()

        assertThat(refilled.weightKg).isEqualTo(65f)
        assertThat(refilled.reps).isEqualTo(10)
    }

    @Test
    fun `a value the user emptied isn't filled in`() {
        // The weight was suggested; the reps were cleared
        val block = block(WorkoutSet(reps = null, weightKg = 60f, suggestedReps = 8, suggestedKg = 60f))

        val refilled = block.withSuggestionsRefilled(sources, Prefill(weightKg = 72f, reps = 8)).set()

        assertThat(refilled.weightKg).isEqualTo(72f)
        assertThat(refilled.reps).isNull()
    }

    @Test
    fun `suggestions turned off empty the suggested values`() {
        val block = block(WorkoutSet(reps = 8, weightKg = 60f, requiredReps = 10, suggestedKg = 60f))

        val refilled = block.withSuggestionsRefilled(sources, Prefill(weightKg = 72f, reps = 8, fills = false)).set()

        assertThat(refilled.weightKg).isNull()
        assertThat(refilled.suggestedKg).isNull()
        // Not a suggestion, so it stays
        assertThat(refilled.reps).isEqualTo(8)
        assertThat(refilled.requiredReps).isEqualTo(10)
    }

    @Test
    fun `done sets keep their values`() {
        val block = block(WorkoutSet(reps = 8, weightKg = 60f, suggestedReps = 8, suggestedKg = 60f, completedDate = 1L))

        val refilled = block.withSuggestionsRefilled(sources, Prefill(weightKg = 72f, reps = 8)).set()

        assertThat(refilled.weightKg).isEqualTo(60f)
    }

    @Test
    fun `an exercise without history is emptied, as there's nothing to fill in from`() {
        val block = block(WorkoutSet(reps = 8, weightKg = 60f, suggestedReps = 8, suggestedKg = 60f))

        val refilled = block.withSuggestionsRefilled(emptyMap(), Prefill(weightKg = 72f, reps = 8)).set()

        assertThat(refilled.weightKg).isNull()
        assertThat(refilled.reps).isNull()
    }

    private fun block(set: WorkoutSet) = WorkoutBlock(
        id = 1,
        uuid = "group",
        type = "SINGLE",
        restSeconds = 60,
        exercises = listOf(bench),
        rounds = listOf(WorkoutRound(id = 1, sets = listOf(set))),
    )

    private fun WorkoutBlock.set() = rounds.single().sets.single()

    // Fills empty values with [weightKg] and [reps], like the history and 1RM prefill
    private class Prefill(val weightKg: Float, val reps: Int, val fills: Boolean = true) : WorkoutSetPrefill {
        override fun fillsSets() = fills

        override fun fill(source: PrefillSource, set: WorkoutSet, index: Int) =
            set.copy(weightKg = set.weightKg ?: weightKg, reps = set.reps ?: reps)

        override fun fillAdded(source: PrefillSource, set: WorkoutSet, index: Int) = fill(source, set, index)
    }
}
