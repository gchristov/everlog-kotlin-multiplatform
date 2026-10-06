package com.everlog.data.controllers.starterroutines

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.everlog.data.model.ELRoutine
import com.everlog.data.repositories.ExerciseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * The starter routines for someone's onboarding answers: generates the week, loads the exercise
 * library and matches the two. Nothing is saved.
 *
 * Fails with the library's load error, [ExerciseLibraryEmptyException] when it loads empty (most
 * likely offline with nothing cached), or [StarterWeek.ExerciseNotFoundException] when a template
 * points at an exercise the library doesn't have. Failures are reported as non-fatals, so the
 * caller only needs to offer a retry or skip.
 */
interface BuildStarterRoutinesUseCase {
    suspend operator fun invoke(dto: Dto): Either<Throwable, List<ELRoutine>>

    data class Dto(val profile: StarterProfile, val createdDate: Long)
}

class RealBuildStarterRoutinesUseCase(
    private val dispatcher: CoroutineDispatcher,
    private val exerciseRepository: ExerciseRepository,
) : BuildStarterRoutinesUseCase {

    override suspend operator fun invoke(
        dto: BuildStarterRoutinesUseCase.Dto
    ): Either<Throwable, List<ELRoutine>> = withContext(dispatcher) {
        either {
            val week = StarterRoutineGenerator.generate(dto.profile)
            val library = exerciseRepository.globalExercises().bind()
            ensure(library.isNotEmpty()) { ExerciseLibraryEmptyException() }
            week.toRoutines(library, dto.createdDate).bind()
        }.onLeft { Timber.tag(TAG).e(it) }
    }

    private companion object {
        const val TAG = "BuildStarterRoutines"
    }
}

class ExerciseLibraryEmptyException : IllegalStateException("The exercise library loaded empty")
