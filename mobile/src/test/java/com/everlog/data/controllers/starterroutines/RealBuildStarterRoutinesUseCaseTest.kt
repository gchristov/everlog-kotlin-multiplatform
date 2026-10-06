package com.everlog.data.controllers.starterroutines

import arrow.core.Either
import com.everlog.data.controllers.starterroutines.StarterProfile.Experience
import com.everlog.data.controllers.starterroutines.StarterProfile.Goal
import com.everlog.data.controllers.starterroutines.StarterProfile.Place
import com.everlog.testutil.FakeCoroutineDispatcher
import com.everlog.testutil.FakeExerciseRepository
import com.everlog.testutil.exerciseLibrary
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException

class RealBuildStarterRoutinesUseCaseTest {

    private val profile = StarterProfile(daysPerWeek = 4, place = Place.GYM, experience = Experience.A_WHILE, goal = Goal.BUILD_MUSCLE)
    private val week = StarterRoutineGenerator.generate(profile)
    private val repository = FakeExerciseRepository()

    @Test
    fun `builds the week's routines from the exercise library`() = runTest { useCase ->
        val library = exerciseLibrary(week)
        repository.globalExercises = Either.Right(library)

        val routines = useCase(BuildStarterRoutinesUseCase.Dto(profile, createdDate = 1_000L)).getOrNull()!!

        assertThat(routines.map { it.name }).containsExactly("Upper", "Lower").inOrder()
        assertThat(routines.map { it.createdDate }.distinct()).containsExactly(1_000L)
        assertThat(routines.flatMap { it.exerciseGroups }.map { it.exercises.single().exercise!!.uuid })
            .containsExactlyElementsIn(week.routines.flatMap { it.exercises }.map { it.exerciseId })
            .inOrder()
        assertThat(repository.globalExercisesCalls).isEqualTo(1)
    }

    @Test
    fun `fails when the exercise library doesn't load`() = runTest { useCase ->
        val error = IOException("Offline")
        repository.globalExercises = Either.Left(error)

        val result = useCase(BuildStarterRoutinesUseCase.Dto(profile, createdDate = 0L))

        assertThat(result).isEqualTo(Either.Left(error))
    }

    @Test
    fun `fails when the exercise library is empty`() = runTest { useCase ->
        repository.globalExercises = Either.Right(emptyList())

        val error = useCase(BuildStarterRoutinesUseCase.Dto(profile, createdDate = 0L)).leftOrNull()

        assertThat(error).isInstanceOf(ExerciseLibraryEmptyException::class.java)
    }

    @Test
    fun `fails when an exercise isn't in the library`() = runTest { useCase ->
        val missing = week.routines.first().exercises.first()
        repository.globalExercises = Either.Right(exerciseLibrary(week).filterNot { it.uuid == missing.exerciseId })

        val error = useCase(BuildStarterRoutinesUseCase.Dto(profile, createdDate = 0L)).leftOrNull()

        assertThat(error).isInstanceOf(StarterWeek.ExerciseNotFoundException::class.java)
        assertThat(error).hasMessageThat().contains(missing.exerciseId)
    }

    private fun runTest(
        testBlock: suspend CoroutineScope.(BuildStarterRoutinesUseCase) -> Unit
    ) = runBlocking {
        testBlock(RealBuildStarterRoutinesUseCase(dispatcher = FakeCoroutineDispatcher, exerciseRepository = repository))
    }
}
