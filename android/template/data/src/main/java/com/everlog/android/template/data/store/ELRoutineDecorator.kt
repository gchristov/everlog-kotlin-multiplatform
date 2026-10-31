package com.everlog.android.template.data.store

import com.everlog.android.auth.data.LocalUserManager
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.firebase.FirestorePaths
import com.everlog.android.exercise.data.store.ExerciseStores
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
        val exerciseMap = ExerciseStores.exercisesStore().getMergedItemsMap()
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
        val exerciseMap = ExerciseStores.exercisesStore().getMergedItemsMap()
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
            val globalRef = FirestorePaths.globalExercises.document(uuid)
            val globalDoc = Tasks.await(globalRef.get(source))
            if (globalDoc.exists()) {
                return globalDoc.toObject(ELExercise::class.java)
            }

            // Try user
            val userRef = try {
                FirestorePaths.exercises(LocalUserManager.requireUserId()).document(uuid)
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
