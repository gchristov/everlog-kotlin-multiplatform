package com.everlog.android.template.data.session

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.optional
import com.everlog.android.common.kotlin.zip
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.exercise.data.model.library.toELExercise
import com.everlog.android.exercise.data.ExerciseRepository
import com.everlog.android.template.data.RoutineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.apache.commons.lang3.SerializationUtils

/**
 * One of the user's routines to edit, as it changes, with each exercise as the library or the user's own exercises
 * have it now, so a rename shows. An exercise neither has, e.g. a deleted own exercise, stays as the routine recorded
 * it, and so do all of them when the exercises can't be read.
 *
 * Unavailable with the routine's sync error, or [TemplateNotFoundException] once it's known not to exist.
 */
interface LoadTemplateUseCase {
    operator fun invoke(uuid: String): Flow<Load<ELRoutine>>
}

class RealLoadTemplateUseCase(
    private val dispatcher: CoroutineDispatcher,
    private val routineRepository: RoutineRepository,
    private val exerciseRepository: ExerciseRepository,
) : LoadTemplateUseCase {

    override operator fun invoke(uuid: String): Flow<Load<ELRoutine>> =
        combine(routineRepository.routine(uuid), exerciseRepository.libraryExercises(), exerciseRepository.ownExercises(), ::templateOf)
            .flowOn(dispatcher)
}

internal fun templateOf(
    routine: Load<ELRoutine?>,
    global: Load<List<LibraryExercise>>,
    own: Load<List<LibraryExercise>>,
): Load<ELRoutine> {
    val exercises = global.optional().zip(own.optional()) { g, o -> g.orEmpty() + o.orEmpty() }
    return when (val joined = routine.zip(exercises) { r, current -> r?.withCurrentExercises(current) }) {
        is Load.Ready -> joined.value?.let { Load.Ready(it) } ?: Load.Unavailable(TemplateNotFoundException())
        Load.Loading -> Load.Loading
        is Load.Unavailable -> joined
    }
}

// A copy, so the cached routine isn't changed
private fun ELRoutine.withCurrentExercises(current: List<LibraryExercise>): ELRoutine {
    val byUuid = current.associate { it.uuid to it.toELExercise() }
    return SerializationUtils.clone(this).apply { resolveExercises(byUuid) }
}

class TemplateNotFoundException : IllegalStateException("The routine doesn't exist")
