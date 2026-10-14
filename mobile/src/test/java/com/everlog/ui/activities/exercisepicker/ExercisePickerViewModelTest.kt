package com.everlog.ui.activities.exercisepicker

import androidx.lifecycle.SavedStateHandle
import arrow.core.Either
import com.everlog.data.controllers.exerciselibrary.LoadExerciseLibraryUseCase
import com.everlog.data.model.exercise.library.ExerciseLibrary
import com.everlog.data.model.exercise.library.LibraryExercise
import com.everlog.data.model.set.ELSetType
import com.everlog.testutil.FakeCoroutineDispatcher
import com.everlog.testutil.LibraryFixtures
import com.everlog.testutil.LibraryFixtures.barbellBench
import com.everlog.testutil.LibraryFixtures.barbellRow
import com.everlog.testutil.LibraryFixtures.dumbbellBench
import com.everlog.testutil.LibraryFixtures.own
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Test

class ExercisePickerViewModelTest {

    private val library = CompletableDeferred<ExerciseLibrary>()
    private val ownUpdates = MutableSharedFlow<List<LibraryExercise>>(extraBufferCapacity = 1)
    private val recent = FakeRecentSearches()

    private fun viewModel(mode: PickerMode = PickerMode.Pick, loaded: Boolean = true): ExercisePickerViewModel {
        if (loaded) library.complete(LibraryFixtures.library())
        return ExercisePickerViewModel(
            dispatcher = FakeCoroutineDispatcher,
            mode = mode,
            savedState = SavedStateHandle(),
            loadLibrary = object : LoadExerciseLibraryUseCase {
                override suspend fun invoke(): Either<Throwable, ExerciseLibrary> = Either.Right(library.await())
            },
            ownExerciseUpdates = { ownUpdates },
            recentSearches = recent,
            analytics = NoAnalytics,
            me = FilterValue(id = CreatedByMe, name = "Me"),
            maxPicks = 2,
            contentDispatcher = FakeCoroutineDispatcher,
        )
    }

    private fun ExercisePickerViewModel.tap(vararg exercises: LibraryExercise) =
        exercises.forEach { onExerciseTapped(it.uuid, section = "movement", position = 0) }

    @Test
    fun `picks up to the limit, then says so`() {
        val viewModel = viewModel()
        viewModel.tap(barbellBench, dumbbellBench, barbellRow)

        assertThat(viewModel.state.value.picked).containsExactly(barbellBench.uuid, dumbbellBench.uuid).inOrder()
        assertThat(viewModel.state.value.message).isEqualTo(PickerMessage.TooMany(2))
    }

    @Test
    fun `adding returns the picks in the order they were picked, with the set type`() {
        val viewModel = viewModel()
        viewModel.tap(dumbbellBench, barbellBench)
        viewModel.onAdd(ELSetType.SUPER)

        assertThat(viewModel.state.value.result).isEqualTo(PickerResult(listOf(dumbbellBench, barbellBench), ELSetType.SUPER))
    }

    @Test
    fun `a set type chosen before the library loads waits for it`() {
        val viewModel = viewModel(loaded = false)
        viewModel.tap(barbellBench)
        viewModel.onAdd(ELSetType.SINGLE)
        assertThat(viewModel.state.value.result).isNull()

        library.complete(LibraryFixtures.library())
        assertThat(viewModel.state.value.result).isEqualTo(PickerResult(listOf(barbellBench), ELSetType.SINGLE))
    }

    @Test
    fun `an exercise just created is picked when picking, not when browsing`() {
        val picking = viewModel()
        picking.onExerciseCreated("new")
        assertThat(picking.state.value.picked).containsExactly("new")

        val browsing = viewModel(mode = PickerMode.Browse)
        browsing.onExerciseCreated("new")
        assertThat(browsing.state.value.picked).isEmpty()
    }

    @Test
    fun `an own exercise deleted elsewhere is unpicked, one just created and still on its way stays`() {
        val viewModel = viewModel()
        viewModel.tap(own)
        viewModel.onExerciseCreated("new")

        ownUpdates.tryEmit(emptyList())
        assertThat(viewModel.state.value.picked).containsExactly("new")
    }

    @Test
    fun `recent keeps only the last filters applied`() {
        val viewModel = viewModel()
        viewModel.onOpenKey(FilterKey.Muscle, source = "chip")
        viewModel.onToggleValue(FilterKey.Muscle, "chest")
        viewModel.onCloseMenu()
        viewModel.onOpenKey(FilterKey.Equipment, source = "chip")
        viewModel.onToggleValue(FilterKey.Equipment, "barbell")
        viewModel.onCloseMenu()

        val last = listOf(FilterToken(FilterKey.Muscle, listOf("chest")), FilterToken(FilterKey.Equipment, listOf("barbell")))
        assertThat(viewModel.state.value.recent).containsExactly(last)
        assertThat(recent.saved).containsExactly(last)
    }

    private class FakeRecentSearches : ExercisePickerRecentSearches {
        var saved: List<List<FilterToken>> = emptyList()

        override fun load() = saved

        override fun save(searches: List<List<FilterToken>>) {
            saved = searches
        }
    }

    private object NoAnalytics : ExercisePickerAnalytics {
        override fun filterAdded(filter: String, value: String, source: String) = Unit
        override fun filterRemoved(filter: String) = Unit
        override fun filterOperatorChanged(filter: String, not: Boolean) = Unit
        override fun searched(words: Int, results: Int) = Unit
        override fun noResults(text: String?, filters: Int) = Unit
        override fun exercisePicked(section: String, position: Int) = Unit
        override fun createTapped(source: String) = Unit
        override fun added(count: Int, setType: String, filters: Int, hadText: Boolean) = Unit
    }
}
