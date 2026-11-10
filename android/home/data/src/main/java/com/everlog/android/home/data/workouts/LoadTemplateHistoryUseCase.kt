package com.everlog.android.home.data.workouts

import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.di.IoDispatcher
import com.everlog.android.common.kotlin.map
import com.everlog.android.workout.data.WorkoutRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * The templates of the last [HistoryMonths] months' workouts as they change, from the recent history the app keeps
 * synced, so it costs no reads of its own. Two months is where the suggestion stops showing more often.
 */
interface LoadTemplateHistoryUseCase {
    operator fun invoke(): Flow<Load<TemplateHistory>>
}

@ContributesBinding(AppScope::class)
@Inject
class RealLoadTemplateHistoryUseCase(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val workoutRepository: WorkoutRepository,
) : LoadTemplateHistoryUseCase {

    override operator fun invoke(): Flow<Load<TemplateHistory>> =
        workoutRepository.recentWorkouts(HistoryMonths)
            .map { load -> load.map(::templateHistory) }
            .flowOn(dispatcher)
}

private const val HistoryMonths = 2
