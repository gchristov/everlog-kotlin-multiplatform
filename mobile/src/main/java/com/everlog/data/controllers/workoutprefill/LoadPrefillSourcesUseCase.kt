package com.everlog.data.controllers.workoutprefill

import arrow.core.Either
import com.everlog.data.controllers.workoutprefill.BaseWorkoutPrefillController.PrefillSource
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.datastores.base.OnStoreItemsListener
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.workout.ELWorkout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * What each exercise's sets are filled in from, by the exercise's uuid: its last session and its best recent 1RM,
 * from the user's workout history. Exercises never done are left out.
 */
interface LoadPrefillSourcesUseCase {
    suspend operator fun invoke(exercises: List<ELExercise>): Either<Throwable, Map<String, PrefillSource>>
}

class RealLoadPrefillSourcesUseCase(
    private val dispatcher: CoroutineDispatcher,
    private val clock: () -> Long = System::currentTimeMillis,
) : LoadPrefillSourcesUseCase {

    override suspend operator fun invoke(exercises: List<ELExercise>): Either<Throwable, Map<String, PrefillSource>> =
        Either.catch {
            val history = history()
            withContext(dispatcher) { WorkoutPrefillController.buildPrefillSources(exercises, history, clock()) }
        }

    private suspend fun history(): List<ELWorkout> = suspendCancellableCoroutine { continuation ->
        ELDatastore.workoutsStore().getItems(object : OnStoreItemsListener<ELWorkout> {
            override fun onItemsLoaded(items: MutableList<ELWorkout>, fromCache: Boolean) {
                // Copied, as the store keeps updating its list
                if (continuation.isActive) continuation.resume(ArrayList(items))
            }

            override fun onItemsLoadingError(throwable: Throwable) {
                if (continuation.isActive) continuation.resumeWithException(throwable)
            }
        })
    }
}
