package com.everlog.data.datastores.routines

import com.everlog.data.datastores.ELDatastore
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.firebase.FirestorePathManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.Source
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
            Timber.tag(TAG).d("ELExercisesStore is empty, attempting cache resolution for routine: %s", routine.uuid)
            val uuids = routine.getExerciseUuids()
            if (uuids.isNotEmpty()) {
                val resolvedMap = resolveExercisesFromCache(uuids)
                if (resolvedMap.isNotEmpty()) {
                    routine.resolveExercises(resolvedMap)
                }
            }
        }
    }

    fun decorate(workout: ELWorkout) {
        val exerciseMap = ELDatastore.exercisesStore().getMergedItemsMap()
        if (exerciseMap.isNotEmpty()) {
            Timber.tag(TAG).d("Decorating workout: %s", workout.uuid)
            workout.resolveExercises(exerciseMap)
        } else {
            Timber.tag(TAG).d("ELExercisesStore is empty, attempting cache resolution for workout: %s", workout.uuid)
            val uuids = workout.getExerciseUuids()
            if (uuids.isNotEmpty()) {
                val resolvedMap = resolveExercisesFromCache(uuids)
                if (resolvedMap.isNotEmpty()) {
                    workout.resolveExercises(resolvedMap)
                }
            }
        }
    }

    private fun resolveExercisesFromCache(uuids: Set<String>): Map<String, ELExercise> {
        val resolved = HashMap<String, ELExercise>()
        uuids.forEach { uuid ->
            val exercise = resolveExercise(uuid, Source.CACHE)
            if (exercise != null) {
                resolved[uuid] = exercise
            }
        }
        return resolved
    }

    private fun resolveExercise(uuid: String, source: Source): ELExercise? {
        return try {
            // Try global
            val globalRef = FirestorePathManager.globalExercisesCollection.document(uuid)
            val globalDoc = Tasks.await(globalRef.get(source))
            if (globalDoc.exists()) {
                return globalDoc.toObject(ELExercise::class.java)
            }

            // Try user
            val userRef = try {
                FirestorePathManager.exercisesCollection.document(uuid)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            if (userRef != null) {
                val userDoc = Tasks.await(userRef.get(source))
                if (userDoc.exists()) {
                    return userDoc.toObject(ELExercise::class.java)
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
