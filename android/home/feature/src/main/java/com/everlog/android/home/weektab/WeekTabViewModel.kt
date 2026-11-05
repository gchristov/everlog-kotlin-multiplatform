package com.everlog.android.home.weektab

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.mvvm.CommonViewModel
import com.everlog.android.home.HomeAnalytics
import com.everlog.android.home.HomeConfig
import com.everlog.android.home.data.HomeNotificationTap
import com.everlog.android.home.data.HomeNotifications
import com.everlog.android.home.data.model.HomeNotification
import com.everlog.android.home.data.tap
import com.everlog.android.home.data.week.weekStats
import com.everlog.android.settings.data.UserSettings
import com.everlog.android.workout.data.WorkoutRepository
import kotlinx.coroutines.CoroutineDispatcher
import org.threeten.bp.LocalDate

/**
 * Home's Week tab: the user's current week (its days, their weekly goal and the week's numbers) from the workouts
 * DataSync keeps, and the banner Remote Config sets. Settings, the date and the banner are read again on [refresh],
 * when the tab shows or they change.
 */
class WeekTabViewModel(
    dispatcher: CoroutineDispatcher,
    workouts: WorkoutRepository,
    private val settings: UserSettings,
    private val notifications: HomeNotifications,
    private val config: HomeConfig,
    private val analytics: HomeAnalytics,
    private val today: () -> LocalDate,
) : CommonViewModel<WeekTabState>(dispatcher, initialState(settings, today())) {

    private var recent: List<ELWorkout> = emptyList()
    private var notification: HomeNotification? = null
    private var shownBannerId: String? = null

    init {
        refreshBanner()
        launchCoroutine {
            workouts.recentWorkouts(RecentMonths).collect { load ->
                when (load) {
                    is Load.Ready -> showWeek(load.value)
                    // Offline before the workouts ever synced: an empty week, as there's nothing to count
                    is Load.Unavailable -> showWeek(emptyList())
                    Load.Loading -> Unit
                }
            }
        }
    }

    fun refresh() {
        setState { withWeek(recent) }
        refreshBanner()
    }

    fun onBannerClosed() {
        val shown = notification ?: return
        analytics.notificationDismissed(shown.title)
        hideBanner(shown)
    }

    // What the tap opens, for the screen to open it
    fun onBannerTapped(): HomeNotificationTap {
        val shown = notification ?: return HomeNotificationTap.Nothing
        analytics.notificationTapped(shown.title)
        val tap = shown.tap(config.appVersionCode)
        // The user acted on it, or it had nothing to do, so it doesn't show again; one asking for an update stays
        if (tap != HomeNotificationTap.UpdateApp) hideBanner(shown)
        return tap
    }

    private fun showWeek(workouts: List<ELWorkout>) {
        recent = workouts
        setState { withWeek(workouts).copy(loading = false) }
    }

    private fun WeekTabState.withWeek(workouts: List<ELWorkout>): WeekTabState {
        val date = today()
        return copy(
            today = date,
            stats = weekStats(workouts, date, settings.firstDayOfWeek(), settings.weightUnit()),
            weeklyGoal = settings.weeklyWorkoutsGoal(),
            weightUnit = settings.weightUnit(),
        )
    }

    private fun refreshBanner() {
        val current = config.notification()?.takeIf(notifications::shouldShow)
        notification = current
        setState { copy(banner = current?.toBanner()) }
        // A refresh shows the same banner again, so only its first showing counts
        if (current != null && current.dismissalId() != shownBannerId) {
            shownBannerId = current.dismissalId()
            analytics.notificationShown(current.title)
        }
    }

    private fun hideBanner(shown: HomeNotification) {
        notifications.dismissed(shown)
        notification = null
        setState { copy(banner = null) }
    }

    private fun HomeNotification.toBanner() = WeekTabBanner(
        title = title.orEmpty(),
        description = description.orEmpty(),
        imageUrl = imageUrl?.takeIf { it.isNotBlank() },
        updateRequired = appUpdateRequired(config.appVersionCode),
    )

    private companion object {
        // Reaches back past the start of any week
        const val RecentMonths = 1

        fun initialState(settings: UserSettings, today: LocalDate) = WeekTabState(
            today = today,
            stats = weekStats(emptyList(), today, settings.firstDayOfWeek(), settings.weightUnit()),
            weeklyGoal = settings.weeklyWorkoutsGoal(),
            weightUnit = settings.weightUnit(),
        )
    }
}
