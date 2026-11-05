package com.everlog.android.statistics.ui.page

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.mvvm.CommonViewModel
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.statistics.data.StatsPeriod
import com.everlog.android.statistics.data.UserStats
import com.everlog.android.statistics.data.userStats
import com.everlog.android.statistics.ui.StatisticsAnalytics
import com.everlog.android.workout.data.WorkoutRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.threeten.bp.LocalDate

data class StatisticsPageState(
    val period: StatsPeriod,
    val stats: UserStats,
    val today: LocalDate,
    val weightUnit: WeightUnit,
    // Until the period's workouts first load
    val loading: Boolean = true,
    // The last 12 months couldn't load, as their first sync didn't finish: try again
    val failed: Boolean = false,
    // Whether there's a feedback form to ask for more statistics with
    val feedback: Boolean = false,
)

/**
 * The Activity tab's statistics page: a week's, month's or the last 12 months' numbers and charts, following the
 * cache, so a workout saved, changed or deleted counts straight away. The week and month come from the workouts
 * DataSync always keeps synced, so they show whatever the connection; the 12 months have a read of their own while
 * picked, whose first sync can fail and be tried again, so the page never holds more than a year. Settings and the
 * date are read again on [refresh].
 */
class StatisticsPageViewModel(
    dispatcher: CoroutineDispatcher,
    private val workouts: WorkoutRepository,
    private val settings: UserSettings,
    private val analytics: StatisticsAnalytics,
    private val feedback: () -> Boolean,
    private val today: () -> LocalDate,
) : CommonViewModel<StatisticsPageState>(dispatcher, initialState(settings, today(), feedback())) {

    private var synced: List<ELWorkout>? = null
    private var year: List<ELWorkout>? = null
    private var yearRead: Job? = null

    init {
        launchCoroutine {
            workouts.syncedWorkouts().collect { load ->
                when (load) {
                    is Load.Ready -> synced = load.value
                    // Offline before the workouts ever synced: nothing to count
                    is Load.Unavailable -> synced = emptyList()
                    Load.Loading -> Unit
                }
                if (state.value.period != StatsPeriod.Last12Months) show()
            }
        }
    }

    fun refresh() {
        setState { copy(feedback = feedback()) }
        show()
    }

    fun onPeriodPicked(period: StatsPeriod) {
        if (period == state.value.period) return
        analytics.statisticsPeriodPicked(period)
        setState { copy(period = period) }
        if (period == StatsPeriod.Last12Months) readYear() else dropYear()
        show()
    }

    fun onTryAgain() {
        readYear()
        show()
    }

    private fun readYear() {
        yearRead?.cancel()
        year = null
        setState { copy(failed = false) }
        yearRead = launchCoroutine {
            workouts.recentWorkouts(YearMonths).collect { load ->
                when (load) {
                    is Load.Ready -> year = load.value
                    is Load.Unavailable -> setState { copy(failed = true) }
                    Load.Loading -> Unit
                }
                show()
            }
        }
    }

    // Another period only needs the synced months, so the year leaves memory
    private fun dropYear() {
        yearRead?.cancel()
        yearRead = null
        year = null
        setState { copy(failed = false) }
    }

    private fun show() {
        val period = state.value.period
        val shown = if (period == StatsPeriod.Last12Months) year else synced
        val date = today()
        setState {
            copy(
                today = date,
                weightUnit = settings.weightUnit(),
                loading = shown == null && !failed,
                stats = userStats(shown.orEmpty(), period, date, settings.firstDayOfWeek(), settings.weightUnit()),
            )
        }
    }

    private companion object {
        const val YearMonths = 12

        fun initialState(settings: UserSettings, today: LocalDate, feedback: Boolean) = StatisticsPageState(
            period = StatsPeriod.Week,
            stats = userStats(emptyList(), StatsPeriod.Week, today, settings.firstDayOfWeek(), settings.weightUnit()),
            today = today,
            weightUnit = settings.weightUnit(),
            feedback = feedback,
        )
    }
}
