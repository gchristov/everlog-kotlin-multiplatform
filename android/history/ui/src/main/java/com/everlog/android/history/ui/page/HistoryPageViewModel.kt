package com.everlog.android.history.ui.page

import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.mvvm.CommonViewModel
import com.everlog.android.history.data.HistoryPreferences
import com.everlog.android.history.data.HistoryView
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.workout.data.WorkoutHistoryRead
import com.everlog.android.workout.data.WorkoutRepository
import com.everlog.android.workout.data.example.exampleWorkouts
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

data class HistoryPageState(
    val view: HistoryView,
    val days: List<HistoryDay>,
    val today: LocalDate,
    // The day picked on the calendar, today until one is
    val selected: LocalDate,
    val firstDayOfWeek: DayOfWeek,
    val weightUnit: WeightUnit,
    // Until the first page loads
    val loading: Boolean = true,
    // An older page is on its way
    val loadingMore: Boolean = false,
    // The first workout is shown: nothing older to load
    val complete: Boolean = false,
    // The first page couldn't load, as the history never synced and the server can't be reached: try again
    val failed: Boolean = false,
    // An older page couldn't load from the server: try again
    val olderFailed: Boolean = false,
    // A day picked on the calendar, for the list to scroll to once
    val scrollTo: LocalDate? = null,
    // A day without workouts was picked, to say so once
    val emptyDayPicked: Boolean = false,
    // Which read the days come from: a new one starts again from the newest, scrolled to the bottom
    val read: Int = 0,
    // Sample days to show, faded, while there are no workouts or the first page couldn't load
    val example: List<HistoryDay>? = null,
)

/**
 * The Activity tab's history page: the user's workouts on a calendar or a list, oldest at the top and newest at the
 * bottom, going back a page at a time as the user scrolls up, as far as the first workout. Follows the cache, so a
 * workout saved or deleted shows in place. The pages read so far stay while the page is away, e.g. on the statistics
 * page or a workout. Leaving the tab drops all but the newest page ([onLeft]), which shows straight away when the
 * user comes back while a new read starts. Settings and the date are read again on [refresh].
 */
class HistoryPageViewModel(
    dispatcher: CoroutineDispatcher,
    private val workouts: WorkoutRepository,
    private val settings: UserSettings,
    private val preferences: HistoryPreferences,
    // The sample workouts' names, e.g. Legs, Push and Pull
    private val exampleNames: List<String>,
    private val today: () -> LocalDate,
) : CommonViewModel<HistoryPageState>(dispatcher, initialState(preferences.view(), settings, today())) {

    private var read: WorkoutHistoryRead? = null
    private var job: Job? = null
    private var reads = 0

    init {
        startReading()
    }

    // When the page shows, and when settings change
    fun refresh() {
        setState { copy(today = today(), firstDayOfWeek = settings.firstDayOfWeek(), weightUnit = settings.weightUnit()) }
        if (read == null) startReading()
    }

    // The user left the tab the page is on: let the older pages go, and read again from the newest when they come back
    fun onLeft() {
        job?.cancel()
        job = null
        read = null
        setState {
            val kept = days.newestPage(WorkoutRepository.HistoryPageSize)
            copy(
                days = kept,
                selected = today,
                loadingMore = false,
                complete = complete && kept.size == days.size,
                failed = false,
                scrollTo = null,
                emptyDayPicked = false,
            )
        }
    }

    fun onTryAgain() {
        job?.cancel()
        setState { copy(loading = true, failed = false) }
        startReading()
    }

    // Opening on it from now on. A day picked on the calendar switches to the list for that day without that.
    fun onViewPicked(view: HistoryView) {
        preferences.setView(view)
        setState { copy(view = view) }
    }

    // The user reached the oldest workout shown
    // Not after an older page failed, which only Try again reads again, so the list doesn't keep retrying by itself
    fun onReachedOldest() {
        val state = state.value
        if (state.loading || state.loadingMore || state.complete || state.olderFailed) return
        read?.loadMore()
    }

    // An older page that couldn't load, read again
    fun onTryOlderAgain() {
        if (state.value.olderFailed) read?.loadMore()
    }

    fun onDayPicked(date: LocalDate) {
        val state = state.value
        if (date.isAfter(state.today)) return
        if (state.days.any { it.date == date }) {
            setState { copy(selected = date, view = HistoryView.List, scrollTo = date) }
        } else {
            setState { copy(selected = date, emptyDayPicked = true) }
        }
    }

    fun onScrolledToDay() {
        setState { copy(scrollTo = null) }
    }

    fun onEmptyDayShown() {
        setState { copy(emptyDayPicked = false) }
    }

    private fun startReading() {
        val history = workouts.history()
        read = history
        setState { copy(read = ++reads) }
        job = launchCoroutine {
            history.loads.collect { load ->
                when (load) {
                    is Load.Ready -> setState {
                        val days = historyDays(load.value.workouts)
                        copy(
                            days = days,
                            loading = false,
                            loadingMore = load.value.loadingMore,
                            complete = load.value.complete,
                            failed = false,
                            olderFailed = load.value.failed,
                            example = if (days.isEmpty()) example() else null,
                        )
                    }
                    is Load.Unavailable -> setState { copy(loading = false, failed = true, example = example()) }
                    Load.Loading -> Unit
                }
            }
        }
    }

    private fun example() = historyDays(exampleWorkouts(today(), exampleNames))

    private companion object {
        fun initialState(view: HistoryView, settings: UserSettings, today: LocalDate) = HistoryPageState(
            view = view,
            days = emptyList(),
            today = today,
            selected = today,
            firstDayOfWeek = settings.firstDayOfWeek(),
            weightUnit = settings.weightUnit(),
        )
    }
}
