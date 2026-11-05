package com.everlog.android.history.ui.page

import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.mvvm.CommonViewModel
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.workout.data.WorkoutHistoryRead
import com.everlog.android.workout.data.WorkoutRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

enum class HistoryView { Calendar, List }

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
    // A day picked on the calendar, for the list to scroll to once
    val scrollTo: LocalDate? = null,
    // A day without workouts was picked, to say so once
    val emptyDayPicked: Boolean = false,
    // Which read the days come from: a new one starts again from the newest, scrolled to the bottom
    val read: Int = 0,
)

/**
 * The Activity tab's history page: the user's workouts on a calendar or a list, oldest at the top and newest at the
 * bottom, going back a page at a time as the user scrolls up, as far as the first workout. Follows the cache, so a
 * workout saved or deleted shows in place. The pages read so far stay while the page is away, e.g. on the statistics
 * page or a workout, until [onLeft] drops them. Settings and the date are read again on [refresh].
 */
class HistoryPageViewModel(
    dispatcher: CoroutineDispatcher,
    private val workouts: WorkoutRepository,
    private val settings: UserSettings,
    private val today: () -> LocalDate,
) : CommonViewModel<HistoryPageState>(dispatcher, initialState(settings, today())) {

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

    // The user left the tab the page is on: let the pages go, and start again from the newest when they come back
    fun onLeft() {
        job?.cancel()
        job = null
        read = null
        setState { initialState(settings, today()).copy(view = view) }
    }

    fun onTryAgain() {
        job?.cancel()
        setState { copy(loading = true, failed = false) }
        startReading()
    }

    fun onViewPicked(view: HistoryView) {
        setState { copy(view = view) }
    }

    // The user reached the oldest workout shown
    fun onReachedOldest() {
        val state = state.value
        if (state.loading || state.loadingMore || state.complete) return
        read?.loadMore()
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
                        copy(
                            days = historyDays(load.value.workouts),
                            loading = false,
                            loadingMore = load.value.loadingMore,
                            complete = load.value.complete,
                            failed = false,
                        )
                    }
                    is Load.Unavailable -> setState { copy(loading = false, failed = true) }
                    Load.Loading -> Unit
                }
            }
        }
    }

    private companion object {
        fun initialState(settings: UserSettings, today: LocalDate) = HistoryPageState(
            view = HistoryView.Calendar,
            days = emptyList(),
            today = today,
            selected = today,
            firstDayOfWeek = settings.firstDayOfWeek(),
            weightUnit = settings.weightUnit(),
        )
    }
}
