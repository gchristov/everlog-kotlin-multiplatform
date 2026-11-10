package com.everlog.android.home.data.workouts

import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.workout.testfixtures.FakeWorkoutRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class RealLoadTemplateHistoryUseCaseTest {

    private val repository = FakeWorkoutRepository()
    private val useCase = RealLoadTemplateHistoryUseCase(FakeCoroutineDispatcher, repository)

    @Test
    fun `reads the last two months of the synced history`() = runBlocking<Unit> {
        repository.recent.value = Load.Ready(workoutsFrom("push", "pull"))

        val history = (useCase().first() as Load.Ready).value

        assertThat(history.order).containsExactly("push", "pull").inOrder()
        assertThat(repository.reads.keys).containsExactly(2)
    }
}
