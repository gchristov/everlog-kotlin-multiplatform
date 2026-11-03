package com.everlog.android.template.data.session

import arrow.core.Either
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.kotlin.di.IoDispatcher
import com.everlog.android.template.data.RoutineRepository
import com.everlog.android.template.data.session.OngoingPlanRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
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

@ContributesBinding(AppScope::class)
@Inject
class RealSaveTemplateUseCase(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val routineRepository: RoutineRepository,
    private val ongoingPlanRepository: OngoingPlanRepository,
) : SaveTemplateUseCase {

    override suspend operator fun invoke(routine: ELRoutine): Either<Throwable, Unit> = withContext(dispatcher) {
        routineRepository.saveRoutines(listOf(routine)).onRight { ongoingPlanRepository.replaceRoutine(routine) }
    }
}
