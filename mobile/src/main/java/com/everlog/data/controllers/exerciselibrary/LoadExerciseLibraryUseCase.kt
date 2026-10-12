package com.everlog.data.controllers.exerciselibrary

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.everlog.data.controllers.starterroutines.ExerciseLibraryEmptyException
import com.everlog.data.model.exercise.library.ExerciseLibrary
import com.everlog.data.model.exercise.library.ExerciseRegistry
import com.everlog.data.repositories.ExerciseRepository
import com.everlog.data.repositories.RoutineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Loads the [ExerciseLibrary]: the global exercises, the user's own, the registry and how often the
 * user's templates use each exercise, all at once.
 *
 * Follows the rules for apps reading the library in tools/exercises/README.md: hidden exercises are
 * left out, and a family the registry doesn't know counts as none. The global exercises are the only
 * part it needs: without the registry it names nothing (exercises still show), and without the
 * user's own exercises or templates it leaves those out.
 *
 * Fails with the global exercises' load error, or [ExerciseLibraryEmptyException] when they load
 * empty (most likely offline with nothing cached).
 */
interface LoadExerciseLibraryUseCase {
    suspend operator fun invoke(): Either<Throwable, ExerciseLibrary>
}

class RealLoadExerciseLibraryUseCase(
    private val dispatcher: CoroutineDispatcher,
    private val exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
) : LoadExerciseLibraryUseCase {

    override suspend operator fun invoke(): Either<Throwable, ExerciseLibrary> = withContext(dispatcher) {
        coroutineScope {
            val registryRead = async { exerciseRepository.registry() }
            val globalRead = async { exerciseRepository.libraryExercises() }
            val ownRead = async { exerciseRepository.ownExercises() }
            val countsRead = async { routineRepository.exerciseTemplateCounts() }
            either {
                val global = globalRead.await().bind()
                ensure(global.isNotEmpty()) { ExerciseLibraryEmptyException() }
                val registry = registryRead.await().getOrElse { warn("registry", it); ExerciseRegistry() }
                val own = ownRead.await().getOrElse { warn("own exercises", it); emptyList() }
                val counts = countsRead.await().getOrElse { warn("template counts", it); emptyMap() }
                ExerciseLibrary(
                    exercises = (global + own).map { exercise ->
                        val unknownFamily = exercise.family != null && registry.families.isNotEmpty() && registry.family(exercise.family) == null
                        if (unknownFamily) exercise.copy(family = null) else exercise
                    },
                    registry = registry,
                    templateCounts = counts,
                )
            }
        }
    }

    private fun warn(what: String, error: Throwable) {
        Timber.tag(TAG).w(error, "Loaded the exercise library without its %s", what)
    }

    private companion object {
        const val TAG = "LoadExerciseLibrary"
    }
}
