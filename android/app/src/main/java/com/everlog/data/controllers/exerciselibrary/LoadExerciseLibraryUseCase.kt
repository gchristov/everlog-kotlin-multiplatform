package com.everlog.data.controllers.exerciselibrary

import arrow.core.getOrElse
import com.everlog.android.common.kotlin.Load
import com.everlog.data.controllers.starterroutines.ExerciseLibraryEmptyException
import com.everlog.data.model.exercise.library.ExerciseLibrary
import com.everlog.data.model.exercise.library.ExerciseRegistry
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.data.repositories.ExerciseRepository
import com.everlog.data.repositories.RoutineRepository
import com.everlog.data.repositories.WorkoutRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

/**
 * The [ExerciseLibrary] as it changes: the global exercises, the user's own, the registry, and how
 * often the user's templates use each exercise and when they last did it, read once.
 *
 * Follows the rules for apps reading the library in tools/exercises/README.md: hidden exercises are
 * left out, and a family the registry doesn't know counts as none. The global exercises are the only
 * part it needs: without the registry it names nothing (exercises still show), and without the
 * user's own exercises, templates or workouts it leaves those out.
 *
 * Unavailable with the global exercises' sync error, or [ExerciseLibraryEmptyException] when the
 * server has none, which would mean they stopped parsing.
 */
interface LoadExerciseLibraryUseCase {
    operator fun invoke(): Flow<Load<ExerciseLibrary>>
}

class RealLoadExerciseLibraryUseCase(
    private val dispatcher: CoroutineDispatcher,
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
    private val workoutRepository: WorkoutRepository,
) : LoadExerciseLibraryUseCase {

    override operator fun invoke(): Flow<Load<ExerciseLibrary>> = flow {
        val (counts, lastDone) = coroutineScope {
            val countsRead = async { routineRepository.exerciseTemplateCounts() }
            val lastDoneRead = async { workoutRepository.exerciseLastDone() }
            countsRead.await().getOrElse { warn("template counts", it); emptyMap() } to
                lastDoneRead.await().getOrElse { warn("recent workouts", it); emptyMap() }
        }
        emitAll(
            combine(exerciseRepository.libraryExercises(), exerciseRepository.ownExercises(), exerciseRepository.registry()) { global, own, registry ->
                exerciseLibraryOf(global, own, registry, counts, lastDone)
            }
        )
    }.flowOn(dispatcher)

    private fun warn(what: String, error: Throwable) {
        Timber.tag(TAG).w(error, "Loaded the exercise library without its %s", what)
    }

    private companion object {
        const val TAG = "LoadExerciseLibrary"
    }
}

// Waits for the optional parts until they've answered, so the library doesn't show unnamed and then jump
internal fun exerciseLibraryOf(
    global: Load<List<LibraryExercise>>,
    own: Load<List<LibraryExercise>>,
    registry: Load<ExerciseRegistry?>,
    templateCounts: Map<String, Int>,
    lastDone: Map<String, Long>,
): Load<ExerciseLibrary> = when {
    global is Load.Unavailable -> global
    global == Load.Loading || own == Load.Loading || registry == Load.Loading -> Load.Loading
    (global as Load.Ready).value.isEmpty() -> Load.Unavailable(ExerciseLibraryEmptyException())
    else -> {
        val knownRegistry = (registry as? Load.Ready)?.value ?: ExerciseRegistry()
        val ownExercises = (own as? Load.Ready)?.value.orEmpty()
        Load.Ready(
            ExerciseLibrary(
                exercises = (global.value + ownExercises).map { exercise ->
                    val unknownFamily = exercise.family != null && knownRegistry.families.isNotEmpty() && knownRegistry.family(exercise.family) == null
                    if (unknownFamily) exercise.copy(family = null) else exercise
                },
                registry = knownRegistry,
                templateCounts = templateCounts,
                lastDone = lastDone,
            )
        )
    }
}
