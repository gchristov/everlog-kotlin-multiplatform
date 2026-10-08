package com.everlog.data.controllers.starterroutines

import arrow.core.Either
import com.everlog.data.model.ELRoutine
import com.everlog.testutil.FakeCoroutineDispatcher
import com.everlog.testutil.FakeRoutineRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException

class RealSaveStarterRoutinesUseCaseTest {

    private val routines = listOf(ELRoutine(uuid = "upper", name = "Upper"), ELRoutine(uuid = "lower", name = "Lower"))
    private val repository = FakeRoutineRepository()

    @Test
    fun `saves all the routines in one go`() = runTest { useCase ->
        val result = useCase(SaveStarterRoutinesUseCase.Dto(routines))

        assertThat(result).isEqualTo(Either.Right(Unit))
        assertThat(repository.saved).containsExactly(routines)
    }

    @Test
    fun `fails when the routines don't save`() = runTest { useCase ->
        val error = IOException("Permission denied")
        repository.saveRoutines = Either.Left(error)

        val result = useCase(SaveStarterRoutinesUseCase.Dto(routines))

        assertThat(result).isEqualTo(Either.Left(error))
    }

    @Test
    fun `fails without saving when there are no routines`() = runTest { useCase ->
        val error = useCase(SaveStarterRoutinesUseCase.Dto(emptyList())).leftOrNull()

        assertThat(error).isInstanceOf(StarterRoutinesEmptyException::class.java)
        assertThat(repository.saved).isEmpty()
    }

    private fun runTest(
        testBlock: suspend CoroutineScope.(SaveStarterRoutinesUseCase) -> Unit
    ) = runBlocking {
        testBlock(RealSaveStarterRoutinesUseCase(dispatcher = FakeCoroutineDispatcher, routineRepository = repository))
    }
}
