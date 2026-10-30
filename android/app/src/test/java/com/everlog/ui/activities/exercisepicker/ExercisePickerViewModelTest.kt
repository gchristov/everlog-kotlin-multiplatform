package com.everlog.ui.activities.exercisepicker

import androidx.lifecycle.SavedStateHandle
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.exercise.data.model.library.ExerciseLibrary
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.exercise.testfixtures.LibraryFixtures
import com.everlog.android.exercise.testfixtures.LibraryFixtures.barbellBench
import com.everlog.android.exercise.testfixtures.LibraryFixtures.barbellRow
import com.everlog.android.exercise.testfixtures.LibraryFixtures.dumbbellBench
import com.everlog.android.exercise.testfixtures.LibraryFixtures.own
import com.everlog.data.controllers.exerciselibrary.LoadExerciseLibraryUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test

class ExercisePickerViewModelTest {

    private val library = MutableStateFlow<Load<ExerciseLibrary>>(Load.Loading)
    private val recent = FakeRecentSearches()
    private val analytics = RecordingAnalytics()
    // Loads that fail before one succeeds
    private var failures = 0

    private fun viewModel(
        mode: PickerMode = PickerMode.Pick,
        loaded: Boolean = true,
        savedState: SavedStateHandle = SavedStateHandle(),
    ): ExercisePickerViewModel {
        if (loaded) library.value = Load.Ready(LibraryFixtures.library())
        return ExercisePickerViewModel(
            dispatcher = FakeCoroutineDispatcher,
            mode = mode,
            savedState = savedState,
            loadLibrary = object : LoadExerciseLibraryUseCase {
                override fun invoke(): Flow<Load<ExerciseLibrary>> =
                    if (failures-- > 0) flowOf(Load.Unavailable(IllegalStateException("offline"))) else library
            },
            recentSearches = recent,
            analytics = analytics,
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

        library.value = Load.Ready(LibraryFixtures.library())
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

        val withoutOwn = LibraryFixtures.library().let { it.copy(exercises = it.exercises.filterNot { exercise -> exercise.own }) }
        library.value = Load.Ready(withoutOwn)
        assertThat(viewModel.state.value.picked).containsExactly("new")
    }

    @Test
    fun `a library that changes while the picker is open shows the change`() {
        val viewModel = viewModel()
        val renamed = barbellRow.copy(name = "Bent-over Row")

        library.value = Load.Ready(LibraryFixtures.library().let { it.copy(exercises = it.exercises.map { exercise -> if (exercise.uuid == barbellRow.uuid) renamed else exercise }) })

        assertThat(viewModel.state.value.index!!.library.exercises).contains(renamed)
    }

    @Test
    fun `the shimmer shows until the library arrives`() {
        val viewModel = viewModel(loaded = false)
        assertThat(viewModel.state.value.loading).isTrue()

        library.value = Load.Ready(LibraryFixtures.library())

        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.content).isNotNull()
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

    @Test
    fun `the screen view says whether it's picking or browsing`() {
        viewModel().onResume()
        viewModel(mode = PickerMode.Browse).onResume()

        assertThat(analytics.events).containsExactly("screen Pick", "screen Browse").inOrder()
    }

    @Test
    fun `a tap unpicks a picked exercise, and adding with nothing picked does nothing`() {
        val viewModel = viewModel()
        viewModel.tap(barbellBench, barbellBench)
        assertThat(viewModel.state.value.picked).isEmpty()

        viewModel.onAdd(ELSetType.SINGLE)

        assertThat(viewModel.state.value.result).isNull()
        assertThat(analytics.events.filter { it.startsWith("added") }).isEmpty()
    }

    @Test
    fun `browsing, a tap is logged but picks nothing`() {
        val viewModel = viewModel(mode = PickerMode.Browse)
        viewModel.tap(barbellBench)

        assertThat(viewModel.state.value.picked).isEmpty()
        assertThat(analytics.events).contains("picked movement 0")
    }

    @Test
    fun `a library that doesn't load can be tried again`() {
        failures = 1
        val viewModel = viewModel()
        assertThat(viewModel.state.value.failed).isTrue()
        assertThat(viewModel.state.value.content).isNull()

        viewModel.onRetry()

        assertThat(viewModel.state.value.failed).isFalse()
        assertThat(viewModel.state.value.content).isNotNull()
    }

    @Test
    fun `the search and picks come back after process death`() {
        val savedState = SavedStateHandle()
        val before = viewModel(savedState = savedState)
        before.tap(barbellBench)
        before.onOpenKey(FilterKey.Equipment, source = "chip")
        before.onToggleValue(FilterKey.Equipment, "barbell")

        val after = viewModel(savedState = savedState)

        assertThat(after.state.value.picked).containsExactly(barbellBench.uuid)
        assertThat(after.state.value.tokens).containsExactly(FilterToken(FilterKey.Equipment, listOf("barbell")))
    }

    @Test
    fun `filters are logged with where they came from`() {
        val viewModel = viewModel()
        viewModel.onOpenKey(FilterKey.Equipment, source = "chip")
        viewModel.onToggleValue(FilterKey.Equipment, "barbell")
        viewModel.onToggleOperator(FilterKey.Equipment)
        viewModel.onToggleValue(FilterKey.Equipment, "barbell")
        viewModel.onRecent(listOf(FilterToken(FilterKey.Muscle, listOf("chest"))))
        viewModel.onClearAll()

        assertThat(analytics.events.filter { it.startsWith("filter") }).containsExactly(
            "filter_added equipment barbell chip",
            "filter_operator equipment true",
            "filter_removed equipment",
            "filter_added muscle chest recent",
            "filter_removed muscle",
        ).inOrder()
    }

    @Test
    fun `a search with no results is logged once`() {
        val viewModel = viewModel()
        viewModel.onOpenKey(FilterKey.Equipment, source = "chip")
        viewModel.onToggleValue(FilterKey.Equipment, "barbell")
        viewModel.onToggleValue(FilterKey.Muscle, "legs")
        viewModel.onToggleShowAll()

        assertThat(analytics.events.filter { it.startsWith("no_results") }).containsExactly("no_results null 2")
    }

    private class FakeRecentSearches : ExercisePickerRecentSearches {
        var saved: List<List<FilterToken>> = emptyList()

        override fun load() = saved

        override fun save(searches: List<List<FilterToken>>) {
            saved = searches
        }
    }

    private class RecordingAnalytics : ExercisePickerAnalytics {
        val events = mutableListOf<String>()

        override fun screenViewed(mode: PickerMode) {
            events += "screen $mode"
        }

        override fun filterAdded(filter: String, value: String, source: String) {
            events += "filter_added $filter $value $source"
        }

        override fun filterRemoved(filter: String) {
            events += "filter_removed $filter"
        }

        override fun filterOperatorChanged(filter: String, not: Boolean) {
            events += "filter_operator $filter $not"
        }

        override fun searched(words: Int, results: Int) {
            events += "searched $words $results"
        }

        override fun noResults(text: String?, filters: Int) {
            events += "no_results $text $filters"
        }

        override fun exercisePicked(section: String, position: Int) {
            events += "picked $section $position"
        }

        override fun createTapped(source: String) {
            events += "create $source"
        }

        override fun added(count: Int, setType: String, filters: Int, hadText: Boolean) {
            events += "added $count $setType"
        }
    }
}
