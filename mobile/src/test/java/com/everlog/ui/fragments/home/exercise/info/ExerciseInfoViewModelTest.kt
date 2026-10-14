package com.everlog.ui.fragments.home.exercise.info

import arrow.core.Either
import com.everlog.data.controllers.exerciseguide.LoadExerciseGuideUseCase
import com.everlog.data.model.exercise.library.ExerciseGuide
import com.everlog.testutil.FakeCoroutineDispatcher
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException

class ExerciseInfoViewModelTest {

    private val bench = ExerciseInfoViewModel.Exercise(uuid = "bench", name = "Barbell Bench Press", imageUrl = "https://example.com/bench.webp", own = false)
    private val guide = ExerciseGuide(instructions = listOf("Lie on the bench.", "Press."), tips = listOf("Feet flat."))

    @Test
    fun `a library exercise shows its guide once it loads`() {
        val loadGuide = FakeLoadExerciseGuideUseCase(Either.Right(guide))

        val state = viewModel(bench, loadGuide).state.value

        assertThat(state.exercise).isEqualTo(bench)
        assertThat(state.guide).isEqualTo(guide)
        assertThat(loadGuide.reads).containsExactly("bench")
    }

    @Test
    fun `a user's own exercise doesn't read the library and shows only its image`() {
        val loadGuide = FakeLoadExerciseGuideUseCase(Either.Right(guide))

        val state = viewModel(bench.copy(own = true), loadGuide).state.value

        assertThat(state.guide.isEmpty).isTrue()
        assertThat(loadGuide.reads).isEmpty()
    }

    @Test
    fun `a guide that doesn't load leaves only the image`() {
        val state = viewModel(bench, FakeLoadExerciseGuideUseCase(Either.Left(IOException("offline")))).state.value

        assertThat(state.guide.isEmpty).isTrue()
    }

    private fun viewModel(exercise: ExerciseInfoViewModel.Exercise, loadGuide: LoadExerciseGuideUseCase) =
        ExerciseInfoViewModel(dispatcher = FakeCoroutineDispatcher, loadGuide = loadGuide, exercise = exercise)

    private class FakeLoadExerciseGuideUseCase(private val result: Either<Throwable, ExerciseGuide>) : LoadExerciseGuideUseCase {
        val reads = mutableListOf<String>()

        override suspend fun invoke(uuid: String): Either<Throwable, ExerciseGuide> {
            reads += uuid
            return result
        }
    }
}
