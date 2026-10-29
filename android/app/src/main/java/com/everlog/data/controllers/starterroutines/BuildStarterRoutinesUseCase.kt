package com.everlog.data.controllers.starterroutines

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.everlog.data.model.ELRoutine
import com.everlog.data.repositories.ExerciseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The starter routines for someone's onboarding answers: generates the week, loads the exercise
 * library and matches the two. Nothing is saved. The result keeps the generated week too, for what
 * the routines don't hold, like the days each one is trained on and the rep ranges.
 *
 * Fails with the library's load error, [ExerciseLibraryEmptyException] when it loads empty (most
 * likely offline with nothing cached), or [StarterWeek.ExerciseNotFoundException] when a template
 * points at an exercise the library doesn't have. Reporting failures is up to the caller.
 */
interface BuildStarterRoutinesUseCase {
    suspend operator fun invoke(dto: Dto): Either<Throwable, Result>

    data class Dto(val profile: StarterProfile, val createdDate: Long)

    // The routines are in the same order as the week's
    data class Result(val week: StarterWeek, val routines: List<ELRoutine>)
}

class RealBuildStarterRoutinesUseCase(
    private val dispatcher: CoroutineDispatcher,
    private val exerciseRepository: ExerciseRepository,
) : BuildStarterRoutinesUseCase {

    override suspend operator fun invoke(
        dto: BuildStarterRoutinesUseCase.Dto
    ): Either<Throwable, BuildStarterRoutinesUseCase.Result> = withContext(dispatcher) {
        either {
            val week = StarterRoutineGenerator.generate(dto.profile)
            val library = exerciseRepository.globalExercises().bind()
            ensure(library.isNotEmpty()) { ExerciseLibraryEmptyException() }
            BuildStarterRoutinesUseCase.Result(week, week.toRoutines(library, dto.createdDate).bind())
        }
    }
}

class ExerciseLibraryEmptyException : IllegalStateException("The exercise library loaded empty")
