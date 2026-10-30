package com.everlog.android.exercise.data

import com.everlog.android.common.kotlin.Load
import com.everlog.android.exercise.data.model.library.ExerciseLibrary
import com.everlog.android.exercise.data.model.library.ExerciseRegistry
import com.everlog.android.exercise.data.model.library.LibraryExercise
import kotlinx.coroutines.flow.Flow

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

// Waits for the optional parts until they've answered, so the library doesn't show unnamed and then jump
fun exerciseLibraryOf(
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

class ExerciseLibraryEmptyException : IllegalStateException("The exercise library loaded empty")
