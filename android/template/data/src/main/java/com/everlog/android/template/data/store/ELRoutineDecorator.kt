package com.everlog.android.template.data.store

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.exercise.data.ExerciseLookup
import com.everlog.android.exercise.data.store.ExerciseStores

/**
 * Shows a routine's or workout's exercises as they are now, e.g. renamed or with a new image, rather than the copies
 * saved in it. One the lookup doesn't have keeps its saved copy.
 */
class ELRoutineDecorator(private val lookup: ExerciseLookup = ExerciseStores.lookup.value) {

    fun decorate(routine: ELRoutine) {
        routine.resolveExercises(lookup.find(routine.getExerciseUuids()))
    }

    fun decorate(workout: ELWorkout) {
        workout.resolveExercises(lookup.find(workout.getExerciseUuids()))
    }
}
