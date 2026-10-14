package com.everlog.data.repositories

import arrow.core.Either
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.firebase.FirestorePathManager
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

interface WorkoutRepository {
    /**
     * When the user last did each exercise, by the exercise's uuid, from their [workouts] most
     * recent workouts.
     */
    suspend fun exerciseLastDone(workouts: Long = RecentWorkouts): Either<Throwable, Map<String, Long>>

    companion object {
        const val RecentWorkouts = 30L
    }
}

class RealWorkoutRepository(
    private val dispatcher: CoroutineDispatcher,
) : WorkoutRepository {

    override suspend fun exerciseLastDone(workouts: Long): Either<Throwable, Map<String, Long>> = withContext(dispatcher) {
        Either.catch {
            // The history store keeps the cache current, and a new user's history is empty
            val recent = FirestorePathManager.workoutsCollection
                .orderBy("createdDate", Query.Direction.DESCENDING)
                .limit(workouts)
                .getCacheFirst(emptyIsAnswer = true)
                .documents
                .mapNotNull { document ->
                    runCatching { document.toObject(ELWorkout::class.java) }
                        .onFailure { Timber.tag(TAG).w(it, "Skipped workout %s, which doesn't parse", document.id) }
                        .getOrNull()
                }
            exerciseLastDone(recent)
        }
    }

    private companion object {
        const val TAG = "WorkoutRepository"
    }
}

// When each exercise was last done in these workouts, by the exercise's uuid
internal fun exerciseLastDone(workouts: List<ELWorkout>): Map<String, Long> = buildMap {
    workouts.forEach { workout ->
        workout.routine?.exerciseGroups.orEmpty()
            .flatMap { group -> group.exercises.mapNotNull { it.exercise?.uuid } }
            .forEach { uuid -> if (workout.createdDate > (get(uuid) ?: Long.MIN_VALUE)) put(uuid, workout.createdDate) }
    }
}
