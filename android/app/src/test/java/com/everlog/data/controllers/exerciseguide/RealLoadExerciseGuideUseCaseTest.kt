package com.everlog.data.controllers.exerciseguide

import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.exercise.data.model.library.ExerciseGuide
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.testutil.FakeExerciseRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException

class RealLoadExerciseGuideUseCaseTest {

    private val repository = FakeExerciseRepository()
    private val useCase = RealLoadExerciseGuideUseCase(FakeCoroutineDispatcher, repository)

    @Test
    fun `the guide is the library exercise's steps and tips, in order`() = runBlocking<Unit> {
        repository.libraryExercise = Load.Ready(
            LibraryExercise(uuid = "bench", name = "Barbell Bench Press", instructions = listOf("Lie on the bench.", "Press."), tips = listOf("Feet flat.")),
        )

        assertThat(guide("bench")).isEqualTo(ExerciseGuide(instructions = listOf("Lie on the bench.", "Press."), tips = listOf("Feet flat.")))
        assertThat(repository.libraryExerciseReads).containsExactly("bench")
    }

    @Test
    fun `blank steps and tips are left out and the rest trimmed`() = runBlocking<Unit> {
        repository.libraryExercise = Load.Ready(
            LibraryExercise(uuid = "bench", name = "Barbell Bench Press", instructions = listOf(" Lie on the bench. ", "", "  "), tips = listOf("")),
        )

        val guide = guide("bench")

        assertThat(guide.instructions).containsExactly("Lie on the bench.")
        assertThat(guide.tips).isEmpty()
    }

    @Test
    fun `an exercise the library doesn't have, like a user's own, has an empty guide`() = runBlocking<Unit> {
        repository.libraryExercise = Load.Ready(null)

        assertThat(guide("own").isEmpty).isTrue()
    }

    @Test
    fun `an exercise without instructions or tips has an empty guide`() = runBlocking<Unit> {
        repository.libraryExercise = Load.Ready(LibraryExercise(uuid = "old", name = "Cable Crunch"))

        assertThat(guide("old").isEmpty).isTrue()
    }

    @Test
    fun `unavailable when the library is`() = runBlocking<Unit> {
        val unavailable = Load.Unavailable(IOException("offline"))
        repository.libraryExercise = unavailable

        assertThat(useCase("bench").first()).isEqualTo(unavailable)
    }

    private suspend fun guide(uuid: String) = (useCase(uuid).first() as Load.Ready).value
}
