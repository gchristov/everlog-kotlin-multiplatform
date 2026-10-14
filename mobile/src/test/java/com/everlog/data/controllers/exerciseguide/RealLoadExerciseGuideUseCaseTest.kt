package com.everlog.data.controllers.exerciseguide

import arrow.core.Either
import com.everlog.data.model.exercise.library.ExerciseGuide
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.testutil.FakeCoroutineDispatcher
import com.everlog.testutil.FakeExerciseRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException

class RealLoadExerciseGuideUseCaseTest {

    private val repository = FakeExerciseRepository()
    private val useCase = RealLoadExerciseGuideUseCase(FakeCoroutineDispatcher, repository)

    @Test
    fun `the guide is the library exercise's steps and tips, in order`() = runBlocking<Unit> {
        repository.libraryExercise = Either.Right(
            LibraryExercise(uuid = "bench", name = "Barbell Bench Press", instructions = listOf("Lie on the bench.", "Press."), tips = listOf("Feet flat.")),
        )

        assertThat(useCase("bench").getOrNull()).isEqualTo(ExerciseGuide(instructions = listOf("Lie on the bench.", "Press."), tips = listOf("Feet flat.")))
        assertThat(repository.libraryExerciseReads).containsExactly("bench")
    }

    @Test
    fun `blank steps and tips are left out and the rest trimmed`() = runBlocking<Unit> {
        repository.libraryExercise = Either.Right(
            LibraryExercise(uuid = "bench", name = "Barbell Bench Press", instructions = listOf(" Lie on the bench. ", "", "  "), tips = listOf("")),
        )

        val guide = useCase("bench").getOrNull()!!

        assertThat(guide.instructions).containsExactly("Lie on the bench.")
        assertThat(guide.tips).isEmpty()
    }

    @Test
    fun `an exercise the library doesn't have, like a user's own, has an empty guide`() = runBlocking<Unit> {
        repository.libraryExercise = Either.Right(null)

        assertThat(useCase("own").getOrNull()!!.isEmpty).isTrue()
    }

    @Test
    fun `an exercise without instructions or tips has an empty guide`() = runBlocking<Unit> {
        repository.libraryExercise = Either.Right(LibraryExercise(uuid = "old", name = "Cable Crunch"))

        assertThat(useCase("old").getOrNull()!!.isEmpty).isTrue()
    }

    @Test
    fun `fails when the library read fails`() = runBlocking<Unit> {
        val error = IOException("offline")
        repository.libraryExercise = Either.Left(error)

        assertThat(useCase("bench").leftOrNull()).isSameInstanceAs(error)
    }
}
