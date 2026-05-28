package com.everlog.data.datastores.routines

import com.everlog.data.datastores.ELDatastore
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.workout.ELWorkout
import timber.log.Timber

/**
 * Class used to decorate a routine or workout.
 * It hydrates the exercises within the routine using the latest data from ELExercisesStore.
 */
class ELRoutineDecorator {

    private val TAG = "ELRoutineDecorator"

    fun decorate(routine: ELRoutine) {
        val exerciseMap = ELDatastore.exercisesStore().getMergedItemsMap()
        if (exerciseMap.isNotEmpty()) {
            Timber.tag(TAG).d("Decorating routine: %s", routine.uuid)
            routine.resolveExercises(exerciseMap)
        } else {
            // If the store is not yet ready, we might want to observe it,
            // but for simple decoration on load, we rely on the store being populated
            // or the next refresh triggering a re-decoration.
            Timber.tag(TAG).w("ELExercisesStore is empty, skipping decoration for routine: %s", routine.uuid)
        }
    }

    fun decorate(workout: ELWorkout) {
        val exerciseMap = ELDatastore.exercisesStore().getMergedItemsMap()
        if (exerciseMap.isNotEmpty()) {
            Timber.tag(TAG).d("Decorating workout: %s", workout.uuid)
            workout.resolveExercises(exerciseMap)
        } else {
            Timber.tag(TAG).w("ELExercisesStore is empty, skipping decoration for workout: %s", workout.uuid)
        }
    }
}
