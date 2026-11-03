package com.everlog.android.workout.data.prefill

import arrow.core.Either
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.datasync.Range
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.di.DefaultDispatcher
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.workout.data.WorkoutRepository
import com.everlog.android.workout.data.prefill.BaseWorkoutPrefillController.PrefillSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
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

@ContributesBinding(AppScope::class)
class RealLoadPrefillSourcesUseCase(
    private val dispatcher: CoroutineDispatcher,
    private val workoutRepository: WorkoutRepository,
    private val settings: UserSettings,
    private val clock: () -> Long,
) : LoadPrefillSourcesUseCase {

    @Inject
    constructor(
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
        workoutRepository: WorkoutRepository,
        settings: UserSettings,
    ) : this(dispatcher, workoutRepository, settings, System::currentTimeMillis)

    // An exercise's last session can be from any time, so this reads the whole history: the first time after an
    // install, once it has synced
    override suspend operator fun invoke(exercises: List<ELExercise>): Either<Throwable, Map<String, PrefillSource>> =
        when (val history = workoutRepository.workouts(Range.All).first { it != Load.Loading }) {
            is Load.Ready -> Either.Right(withContext(dispatcher) { WorkoutPrefillController.buildPrefillSources(exercises, history.value, clock(), settings.weightUnit()) })
            is Load.Unavailable -> Either.Left(history.error)
            Load.Loading -> error("Filtered out")
        }
}
