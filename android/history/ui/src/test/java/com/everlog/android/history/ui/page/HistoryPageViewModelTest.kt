package com.everlog.android.history.ui.page

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.datatest.loggedSet
import com.everlog.android.common.datatest.workout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.history.data.HistoryPreferences
import com.everlog.android.history.data.HistoryView
import com.everlog.android.settings.testfixtures.FakeUserSettings
import com.everlog.android.workout.data.WorkoutHistory
import com.everlog.android.workout.data.WorkoutRepository
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

    private val preferences = FakeHistoryPreferences()

    private fun viewModel() = HistoryPageViewModel(FakeCoroutineDispatcher, workouts, settings, preferences, listOf("Legs", "Push", "Pull")) { today }

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
    fun `opens on the list, loading, with today picked`() {
        val state = viewModel().state.value

        assertThat(state.view).isEqualTo(HistoryView.List)
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
    fun `a set edited elsewhere shows in place`() {
        val viewModel = viewModel()
        val squat = ELExercise(uuid = "squat", name = "Squat")
        val completed = today.minusDays(1).atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        shown(workout(completed, squat to listOf(loggedSet(reps = 15, weightKg = 10f))))

        shown(workout(completed, squat to listOf(loggedSet(reps = 15, weightKg = 20f))))

        assertThat(viewModel.state.value.days.single().workouts.single().getTotalWeight(WeightUnit.KILOGRAM)).isEqualTo(300f)
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
        preferences.saved = HistoryView.Calendar
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
    fun `leaving keeps the newest page, shown while coming back reads again from the newest in the same view`() {
        val viewModel = viewModel()
        shown(newest, older, complete = true)
        viewModel.onViewPicked(HistoryView.List)
        val firstRead = viewModel.state.value.read

        viewModel.onLeft()

        assertThat(viewModel.state.value.days.map { it.date }).containsExactly(today.minusDays(10), today.minusDays(1)).inOrder()
        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.complete).isTrue()
        assertThat(viewModel.state.value.view).isEqualTo(HistoryView.List)
        assertThat(workouts.historyReads).isEqualTo(1)

        viewModel.refresh()

        assertThat(workouts.historyReads).isEqualTo(2)
        assertThat(viewModel.state.value.read).isGreaterThan(firstRead)
        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.days).hasSize(2)
    }

    @Test
    fun `leaving lets the older pages go, so there's more to read again`() {
        val viewModel = viewModel()
        val pages = List(WorkoutRepository.HistoryPageSize + 10) { finishedOn(today.minusDays(it.toLong() + 1)) }
        shown(*pages.toTypedArray(), complete = true)

        viewModel.onLeft()

        assertThat(viewModel.state.value.days).hasSize(WorkoutRepository.HistoryPageSize)
        assertThat(viewModel.state.value.days.last().date).isEqualTo(today.minusDays(1))
        assertThat(viewModel.state.value.complete).isFalse()
    }

    @Test
    fun `leaving forgets the day picked`() {
        val viewModel = viewModel()
        shown(newest, older)
        viewModel.onDayPicked(today.minusDays(10))

        viewModel.onLeft()

        assertThat(viewModel.state.value.selected).isEqualTo(today)
        assertThat(viewModel.state.value.scrollTo).isNull()
    }

    @Test
    fun `showing again keeps the pages read so far`() {
        val viewModel = viewModel()
        shown(newest)

        viewModel.refresh()

        assertThat(workouts.historyReads).isEqualTo(1)
    }

    @Test
    fun `no workouts shows sample days, by name, until the first one`() {
        val viewModel = viewModel()
        assertThat(viewModel.state.value.example).isNull()

        shown(complete = true)

        val example = viewModel.state.value.example!!
        assertThat(example.last().date).isAtMost(today)
        assertThat(example.flatMap { it.workouts }.mapNotNull { it.getName() }.toSet()).containsExactly("Legs", "Push", "Pull")

        shown(newest, complete = true)
        assertThat(viewModel.state.value.example).isNull()
    }

    @Test
    fun `a history that can't load shows the sample days too, and coming back tries again under them`() {
        val viewModel = viewModel()

        workouts.history.loads.value = Load.Unavailable(IOException("offline"))
        assertThat(viewModel.state.value.example).isNotEmpty()

        viewModel.onLeft()
        assertThat(viewModel.state.value.example).isNotEmpty()
        assertThat(viewModel.state.value.failed).isFalse()

        viewModel.refresh()
        shown(newest)
        assertThat(viewModel.state.value.example).isNull()
    }

    @Test
    fun `opens on the view last picked, and a day picked on the calendar doesn't change it`() {
        preferences.saved = HistoryView.Calendar
        val viewModel = viewModel()
        assertThat(viewModel.state.value.view).isEqualTo(HistoryView.Calendar)
        shown(newest)

        viewModel.onDayPicked(today.minusDays(1))
        assertThat(viewModel.state.value.view).isEqualTo(HistoryView.List)
        assertThat(preferences.saved).isEqualTo(HistoryView.Calendar)

        viewModel.onViewPicked(HistoryView.List)
        assertThat(preferences.saved).isEqualTo(HistoryView.List)
    }

    @Test
    fun `settings changed while away show on refresh`() {
        val viewModel = viewModel()
        settings.setFirstDayOfWeek(DayOfWeek.SUNDAY)

        viewModel.refresh()

        assertThat(viewModel.state.value.firstDayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
    }

    @Test
    fun `offline before the history ever synced, it can be tried again`() {
        val viewModel = viewModel()

        workouts.history.loads.value = Load.Unavailable(IOException("offline"))

        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.failed).isTrue()
        assertThat(viewModel.state.value.days).isEmpty()

        workouts.history.loads.value = Load.Loading
        viewModel.onTryAgain()

        assertThat(workouts.historyReads).isEqualTo(2)
        assertThat(viewModel.state.value.loading).isTrue()
        assertThat(viewModel.state.value.failed).isFalse()

        shown(newest)
        assertThat(viewModel.state.value.days).hasSize(1)
    }

    private class FakeHistoryPreferences : HistoryPreferences {
        var saved = HistoryView.List

        override fun view() = saved

        override fun setView(view: HistoryView) {
            saved = view
        }
    }
}
