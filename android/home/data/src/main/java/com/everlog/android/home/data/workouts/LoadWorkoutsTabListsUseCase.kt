package com.everlog.android.home.data.workouts

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.di.IoDispatcher
import com.everlog.android.common.kotlin.optional
import com.everlog.android.common.kotlin.zip
import com.everlog.android.exercise.data.ExerciseRepository
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.exercise.data.model.library.toELExercise
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
import org.apache.commons.lang3.SerializationUtils

// What the Workouts tab lists: the user's templates and plans, each by name, and how many exercises there are to browse
data class WorkoutsTabLists(
    val templates: List<ELRoutine>,
    val plans: List<PlanWithRoutines>,
    // The library's and the user's own; null when either can't be read
    val exercises: Int? = null,
)

/**
 * The user's templates and plans as they change, the plans with the templates their days use. Reads each once, side
 * by side, and joins them here. Each exercise is as the library or the user's own exercises have it now, so a rename
 * shows; one neither has stays as the template recorded it, and so do all of them when the exercises can't be read.
 * Waits for the templates and plans; unavailable if either is.
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
    private val exerciseRepository: ExerciseRepository,
) : LoadWorkoutsTabListsUseCase {

    override operator fun invoke(): Flow<Load<WorkoutsTabLists>> =
        combine(
            routineRepository.routines(),
            planRepository.plans(),
            exerciseRepository.libraryExercises(),
            exerciseRepository.ownExercises(),
        ) { routines, plans, library, own ->
            val current = library.optional().zip(own.optional()) { l, o -> l.orEmpty() + o.orEmpty() }
            val count = library.optional().zip(own.optional()) { l, o -> if (l != null && o != null) l.size + o.size else null }
            routines.zip(plans) { templates, userPlans ->
                val resolved = templates.withCurrentExercises((current as? Load.Ready)?.value.orEmpty())
                WorkoutsTabLists(
                    templates = resolved.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() }),
                    plans = withRoutines(userPlans, resolved).sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.plan.name.orEmpty() }),
                    exercises = (count as? Load.Ready)?.value,
                )
            }
        }.flowOn(dispatcher)
}

// Copies, so the cached routines aren't changed
private fun List<ELRoutine>.withCurrentExercises(current: List<LibraryExercise>): List<ELRoutine> {
    if (current.isEmpty()) return this
    val byUuid = current.associate { it.uuid to it.toELExercise() }
    return map { routine -> SerializationUtils.clone(routine).apply { resolveExercises(byUuid) } }
}
