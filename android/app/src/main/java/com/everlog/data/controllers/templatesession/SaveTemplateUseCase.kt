package com.everlog.data.controllers.templatesession

import arrow.core.Either
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.data.repositories.OngoingPlanRepository
import com.everlog.data.repositories.RoutineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Saves a routine the user built or edited, then puts it in place of the ongoing plan's copy, which the plan keeps
 * of each of its routines. Works offline, like every save. Fails with the repository's error, and then leaves the
 * plan as it was.
 */
interface SaveTemplateUseCase {
    suspend operator fun invoke(routine: ELRoutine): Either<Throwable, Unit>
}

class RealSaveTemplateUseCase(
    private val dispatcher: CoroutineDispatcher,
    private val routineRepository: RoutineRepository,
    private val ongoingPlanRepository: OngoingPlanRepository,
) : SaveTemplateUseCase {

    override suspend operator fun invoke(routine: ELRoutine): Either<Throwable, Unit> = withContext(dispatcher) {
        routineRepository.saveRoutines(listOf(routine)).onRight { ongoingPlanRepository.replaceRoutine(routine) }
    }
}
