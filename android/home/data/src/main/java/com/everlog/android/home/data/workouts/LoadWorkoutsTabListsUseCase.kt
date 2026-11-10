package com.everlog.android.home.data.workouts

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.di.IoDispatcher
import com.everlog.android.common.kotlin.zip
import com.everlog.android.plan.data.PlanRepository
import com.everlog.android.plan.data.PlanWithRoutines
import com.everlog.android.plan.data.withRoutines
import com.everlog.android.template.data.RoutineRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

// What the Workouts tab lists: the user's templates and plans, each by name
data class WorkoutsTabLists(val templates: List<ELRoutine>, val plans: List<PlanWithRoutines>)

/**
 * The user's templates and plans as they change, the plans with the templates their days use. Reads each once, side
 * by side, and joins them here. Waits for both; unavailable if either is.
 */
interface LoadWorkoutsTabListsUseCase {
    operator fun invoke(): Flow<Load<WorkoutsTabLists>>
}

@ContributesBinding(AppScope::class)
@Inject
class RealLoadWorkoutsTabListsUseCase(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val routineRepository: RoutineRepository,
    private val planRepository: PlanRepository,
) : LoadWorkoutsTabListsUseCase {

    override operator fun invoke(): Flow<Load<WorkoutsTabLists>> =
        combine(routineRepository.routines(), planRepository.plans()) { routines, plans ->
            routines.zip(plans) { templates, userPlans ->
                WorkoutsTabLists(
                    templates = templates.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() }),
                    plans = withRoutines(userPlans, templates).sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.plan.name.orEmpty() }),
                )
            }
        }.flowOn(dispatcher)
}
