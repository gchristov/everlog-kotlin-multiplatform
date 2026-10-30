package com.everlog.data.controllers.templatesession

import arrow.core.Either
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.data.repositories.OngoingPlanRepository
import com.everlog.testutil.FakeRoutineRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test

class SaveTemplateUseCaseTest {

    private val routine = ELRoutine(uuid = "push", name = "Push")
    private val plan = FakeOngoingPlan()

    @Test
    fun `saves the routine and puts it in the ongoing plan`() = runBlocking<Unit> {
        val routines = FakeRoutineRepository()

        val result = RealSaveTemplateUseCase(Dispatchers.Unconfined, routines, plan)(routine)

        assertThat(result.isRight()).isTrue()
        assertThat(routines.saved).containsExactly(listOf(routine))
        assertThat(plan.replaced).containsExactly(routine)
    }

    @Test
    fun `a failed save leaves the plan as it was`() = runBlocking<Unit> {
        val error = IllegalStateException("No user")
        val routines = FakeRoutineRepository(saveRoutines = Either.Left(error))

        val result = RealSaveTemplateUseCase(Dispatchers.Unconfined, routines, plan)(routine)

        assertThat(result).isEqualTo(Either.Left(error))
        assertThat(plan.replaced).isEmpty()
    }

    private class FakeOngoingPlan : OngoingPlanRepository {
        val replaced = mutableListOf<ELRoutine>()

        override fun replaceRoutine(routine: ELRoutine) {
            replaced += routine
        }
    }
}
