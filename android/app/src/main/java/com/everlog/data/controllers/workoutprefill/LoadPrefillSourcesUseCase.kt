package com.everlog.data.controllers.workoutprefill

import arrow.core.Either
import com.everlog.android.common.kotlin.Load
import com.everlog.data.controllers.workoutprefill.BaseWorkoutPrefillController.PrefillSource
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.repositories.WorkoutRepository
import com.everlog.data.sync.Range
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
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
    private val workoutRepository: WorkoutRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : LoadPrefillSourcesUseCase {

    // An exercise's last session can be from any time, so this reads the whole history: the first time after an
    // install, once it has synced
    override suspend operator fun invoke(exercises: List<ELExercise>): Either<Throwable, Map<String, PrefillSource>> =
        when (val history = workoutRepository.workouts(Range.All).first { it != Load.Loading }) {
            is Load.Ready -> Either.Right(withContext(dispatcher) { WorkoutPrefillController.buildPrefillSources(exercises, history.value, clock()) })
            is Load.Unavailable -> Either.Left(history.error)
            Load.Loading -> error("Filtered out")
        }
}
