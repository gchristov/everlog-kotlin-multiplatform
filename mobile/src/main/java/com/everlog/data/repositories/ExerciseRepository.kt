package com.everlog.data.repositories

import arrow.core.Either
import com.everlog.data.model.exercise.ELExercise
import com.everlog.managers.firebase.FirestorePathManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface ExerciseRepository {
    /**
     * The global exercise library, the exercises everyone has. A one-off read, unlike
     * ELExercisesStore, which also listens for changes and merges in the user's own exercises.
     */
    suspend fun globalExercises(): Either<Throwable, List<ELExercise>>
}

class RealExerciseRepository(
    private val dispatcher: CoroutineDispatcher,
) : ExerciseRepository {

    override suspend fun globalExercises(): Either<Throwable, List<ELExercise>> = withContext(dispatcher) {
        Either.catch {
            FirestorePathManager.globalExercisesCollection.get().await().toObjects(ELExercise::class.java)
        }
    }
}
