package com.everlog.android.history.ui.page

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.settings.testfixtures.FakeUserSettings
import com.everlog.android.workout.data.WorkoutHistory
import com.everlog.android.workout.testfixtures.FakeWorkoutRepository
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

class HistoryPageViewModelTest {

    private val today = LocalDate.of(2026, 11, 5)
    private val workouts = FakeWorkoutRepository()
    private val settings = FakeUserSettings()

    private fun viewModel() = HistoryPageViewModel(FakeCoroutineDispatcher, workouts, settings) { today }

    private fun finishedOn(day: LocalDate, hour: Int = 18): ELWorkout {
        val completed = day.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return ELWorkout(completedDate = completed).apply { createdDate = completed - 3_600_000 }
    }

    private val newest = finishedOn(today.minusDays(1))
    private val older = finishedOn(today.minusDays(10))

    private fun shown(vararg workouts: ELWorkout, loadingMore: Boolean = false, complete: Boolean = false) {
        this.workouts.history.loads.value = Load.Ready(WorkoutHistory(workouts.toList(), loadingMore, complete))
    }

    @Test
    fun `opens on the calendar, loading, with today picked`() {
        val state = viewModel().state.value

        assertThat(state.view).isEqualTo(HistoryView.Calendar)
        assertThat(state.loading).isTrue()
        assertThat(state.selected).isEqualTo(today)
    }

    @Test
    fun `shows the days oldest first`() {
        val viewModel = viewModel()

        shown(newest, older)

        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.days.map { it.date }).containsExactly(today.minusDays(10), today.minusDays(1)).inOrder()
    }

    @Test
    fun `reaching the oldest day asks for older ones, unless they're on their way or there are none`() {
        val viewModel = viewModel()
        viewModel.onReachedOldest()
        assertThat(workouts.history.loadMoreCalls).isEqualTo(0)

        shown(newest)
        viewModel.onReachedOldest()
        assertThat(workouts.history.loadMoreCalls).isEqualTo(1)

        shown(newest, loadingMore = true)
        viewModel.onReachedOldest()
        assertThat(workouts.history.loadMoreCalls).isEqualTo(1)

        shown(newest, older, complete = true)
        viewModel.onReachedOldest()
        assertThat(workouts.history.loadMoreCalls).isEqualTo(1)
    }

    @Test
    fun `picking a day with workouts shows it in the list, once`() {
        val viewModel = viewModel()
        shown(newest, older)

        viewModel.onDayPicked(today.minusDays(10))

        assertThat(viewModel.state.value.view).isEqualTo(HistoryView.List)
        assertThat(viewModel.state.value.selected).isEqualTo(today.minusDays(10))
        assertThat(viewModel.state.value.scrollTo).isEqualTo(today.minusDays(10))

        viewModel.onScrolledToDay()
        assertThat(viewModel.state.value.scrollTo).isNull()
    }

    @Test
    fun `picking a day without workouts says so and stays on the calendar`() {
        val viewModel = viewModel()
        shown(newest)

        viewModel.onDayPicked(today.minusDays(3))

        assertThat(viewModel.state.value.view).isEqualTo(HistoryView.Calendar)
        assertThat(viewModel.state.value.selected).isEqualTo(today.minusDays(3))
        assertThat(viewModel.state.value.emptyDayPicked).isTrue()

        viewModel.onEmptyDayShown()
        assertThat(viewModel.state.value.emptyDayPicked).isFalse()
    }

    @Test
    fun `days to come can't be picked`() {
        val viewModel = viewModel()
        shown(newest)

        viewModel.onDayPicked(today.plusDays(1))

        assertThat(viewModel.state.value.selected).isEqualTo(today)
        assertThat(viewModel.state.value.emptyDayPicked).isFalse()
    }

    @Test
    fun `leaving drops the pages, and coming back reads again from the newest in the same view`() {
        val viewModel = viewModel()
        shown(newest, older)
        viewModel.onViewPicked(HistoryView.List)
        val firstRead = viewModel.state.value.read

        viewModel.onLeft()

        assertThat(viewModel.state.value.days).isEmpty()
        assertThat(viewModel.state.value.loading).isTrue()
        assertThat(viewModel.state.value.view).isEqualTo(HistoryView.List)
        assertThat(workouts.historyReads).isEqualTo(1)

        viewModel.refresh()

        assertThat(workouts.historyReads).isEqualTo(2)
        assertThat(viewModel.state.value.read).isGreaterThan(firstRead)
        assertThat(viewModel.state.value.days).hasSize(2)
    }

    @Test
    fun `showing again keeps the pages read so far`() {
        val viewModel = viewModel()
        shown(newest)

        viewModel.refresh()

        assertThat(workouts.historyReads).isEqualTo(1)
    }

    @Test
    fun `settings changed while away show on refresh`() {
        val viewModel = viewModel()
        settings.setFirstDayOfWeek(DayOfWeek.SUNDAY)

        viewModel.refresh()

        assertThat(viewModel.state.value.firstDayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
    }

    @Test
    fun `offline before the history ever synced, there's nothing to show`() {
        val viewModel = viewModel()

        workouts.history.loads.value = Load.Unavailable(IOException("offline"))

        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.complete).isTrue()
        assertThat(viewModel.state.value.days).isEmpty()
    }
}
