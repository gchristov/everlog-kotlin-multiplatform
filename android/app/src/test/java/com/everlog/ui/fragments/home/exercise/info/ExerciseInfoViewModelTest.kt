package com.everlog.ui.fragments.home.exercise.info

import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.exercise.data.model.library.ExerciseGuide
import com.everlog.data.controllers.exerciseguide.LoadExerciseGuideUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import java.io.IOException

class ExerciseInfoViewModelTest {

    private val bench = ExerciseInfoViewModel.Exercise(uuid = "bench", name = "Barbell Bench Press", imageUrl = "https://example.com/bench.webp", own = false)
    private val guide = ExerciseGuide(instructions = listOf("Lie on the bench.", "Press."), tips = listOf("Feet flat."))

    @Test
    fun `a library exercise shows its guide once it loads`() {
        val loadGuide = FakeLoadExerciseGuideUseCase(Load.Ready(guide))

        val state = viewModel(bench, loadGuide).state.value

        assertThat(state.exercise).isEqualTo(bench)
        assertThat(state.guide).isEqualTo(guide)
        assertThat(loadGuide.reads).containsExactly("bench")
    }

    @Test
    fun `a user's own exercise doesn't read the library and shows only its image`() {
        val loadGuide = FakeLoadExerciseGuideUseCase(Load.Ready(guide))

        val state = viewModel(bench.copy(own = true), loadGuide).state.value

        assertThat(state.guide.isEmpty).isTrue()
        assertThat(loadGuide.reads).isEmpty()
    }

    @Test
    fun `a guide that doesn't load leaves only the image`() {
        val state = viewModel(bench, FakeLoadExerciseGuideUseCase(Load.Unavailable(IOException("offline")))).state.value

        assertThat(state.guide.isEmpty).isTrue()
    }

    @Test
    fun `a guide changed in the library while the tab is open shows the change`() {
        val loadGuide = FakeLoadExerciseGuideUseCase(Load.Loading)
        val viewModel = viewModel(bench, loadGuide)
        assertThat(viewModel.state.value.guide.isEmpty).isTrue()

        loadGuide.guide.value = Load.Ready(guide)

        assertThat(viewModel.state.value.guide).isEqualTo(guide)
    }

    @Test
    fun `an edited exercise keeps its guide without reading it again`() {
        val loadGuide = FakeLoadExerciseGuideUseCase(Load.Ready(guide))
        val viewModel = viewModel(bench, loadGuide)

        viewModel.updateExercise(bench.copy(name = "Bench Press"))

        assertThat(viewModel.state.value.exercise.name).isEqualTo("Bench Press")
        assertThat(viewModel.state.value.guide).isEqualTo(guide)
        assertThat(loadGuide.reads).containsExactly("bench")
    }

    private fun viewModel(exercise: ExerciseInfoViewModel.Exercise, loadGuide: LoadExerciseGuideUseCase) =
        ExerciseInfoViewModel(dispatcher = FakeCoroutineDispatcher, loadGuide = loadGuide, exercise = exercise)

    private class FakeLoadExerciseGuideUseCase(load: Load<ExerciseGuide>) : LoadExerciseGuideUseCase {
        val reads = mutableListOf<String>()
        val guide = MutableStateFlow(load)

        override fun invoke(uuid: String): Flow<Load<ExerciseGuide>> {
            reads += uuid
            return guide
        }
    }
}
