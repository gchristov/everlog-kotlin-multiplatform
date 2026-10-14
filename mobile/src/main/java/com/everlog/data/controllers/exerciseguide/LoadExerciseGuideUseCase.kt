package com.everlog.data.controllers.exerciseguide

import arrow.core.Either
import com.everlog.data.model.exercise.library.ExerciseGuide
import com.everlog.data.repositories.ExerciseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Loads an exercise's [ExerciseGuide] from the library: empty when the library doesn't have the
 * exercise (a user's own), with blank steps and tips left out.
 *
 * Fails with the library's read error, e.g. offline with the exercise never read before.
 */
interface LoadExerciseGuideUseCase {
    suspend operator fun invoke(uuid: String): Either<Throwable, ExerciseGuide>
}

class RealLoadExerciseGuideUseCase(
    private val dispatcher: CoroutineDispatcher,
    private val exerciseRepository: ExerciseRepository,
) : LoadExerciseGuideUseCase {

    override suspend operator fun invoke(uuid: String): Either<Throwable, ExerciseGuide> = withContext(dispatcher) {
        exerciseRepository.libraryExercise(uuid).map { exercise ->
            ExerciseGuide(
                instructions = exercise?.instructions.orEmpty().cleaned(),
                tips = exercise?.tips.orEmpty().cleaned(),
            )
        }
    }

    private fun List<String>.cleaned() = map { it.trim() }.filter { it.isNotEmpty() }
}
