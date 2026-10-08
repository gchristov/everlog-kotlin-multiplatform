package com.everlog.data.controllers.starterroutines

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.everlog.data.model.ELRoutine
import com.everlog.data.repositories.RoutineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Saves the starter routines from [BuildStarterRoutinesUseCase] as the user's routines. It's all or
 * nothing, like building them: either every routine is saved or none is, so the user never ends up
 * with part of their templates. Saving the same routines again overwrites them rather than adding
 * copies, so a failed save can be tried again.
 *
 * Fails with [StarterRoutinesEmptyException] when there's nothing to save, or with the repository's
 * error. Reporting failures is up to the caller.
 */
interface SaveStarterRoutinesUseCase {
    suspend operator fun invoke(dto: Dto): Either<Throwable, Unit>

    data class Dto(val routines: List<ELRoutine>)
}

class RealSaveStarterRoutinesUseCase(
    private val dispatcher: CoroutineDispatcher,
    private val routineRepository: RoutineRepository,
) : SaveStarterRoutinesUseCase {

    override suspend operator fun invoke(
        dto: SaveStarterRoutinesUseCase.Dto
    ): Either<Throwable, Unit> = withContext(dispatcher) {
        either {
            ensure(dto.routines.isNotEmpty()) { StarterRoutinesEmptyException() }
            routineRepository.saveRoutines(dto.routines).bind()
        }
    }
}

class StarterRoutinesEmptyException : IllegalStateException("There are no starter routines to save")
