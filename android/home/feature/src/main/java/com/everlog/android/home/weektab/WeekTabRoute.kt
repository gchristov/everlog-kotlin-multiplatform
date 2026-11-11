package com.everlog.android.home.weektab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.compose.components.broadcast.LocalBroadcastEffect
import com.everlog.android.common.compose.components.shown.ShownEffect
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.common.mvp.CommonMvp
import com.everlog.android.common.mvp.MvpBroadcasts
import com.everlog.android.common.navigation.Destination
import com.everlog.android.common.navigation.ExerciseLibrary
import com.everlog.android.common.navigation.PlanCongratulations
import com.everlog.android.common.navigation.PlanDetails
import com.everlog.android.common.navigation.PlayStoreListing
import com.everlog.android.common.navigation.SetSuggestions
import com.everlog.android.common.navigation.StartWorkoutConfirmation
import com.everlog.android.common.navigation.TemplateDetails
import com.everlog.android.common.navigation.WebLink
import com.everlog.android.home.HomeGraph
import com.everlog.android.home.HomeScreenNames
import com.everlog.android.home.data.HomeNotificationTap
import com.everlog.android.home.data.model.HomeNotification
import kotlinx.coroutines.Dispatchers
import org.threeten.bp.LocalDate

// What the Week tab asks of Home: another tab, or its start menu
class WeekTabHomeActions(
    val showStatistics: () -> Unit,
    val showSettings: () -> Unit,
    val showPlans: () -> Unit,
    val showStartMenu: () -> Unit,
    // The week shows its own start button while it's empty
    val onWeekEmpty: (Boolean) -> Unit,
)

/**
 * Home's Week tab with what it needs from the app's graph. It reads again when it shows ([shown]: its tab is the
 * current one), and when the user's settings or Remote Config change.
 */
@Composable
fun WeekTabRoute(shown: Boolean, home: WeekTabHomeActions, bottomPadding: Dp) {
    val context = LocalContext.current
    val graph = remember(context) { context.appGraph<HomeGraph>() }
    val viewModel: WeekTabViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                WeekTabViewModel(
                    dispatcher = Dispatchers.Main,
                    workouts = graph.workoutRepository,
                    settings = graph.userSettings,
                    planProgress = graph.planProgress,
                    notifications = graph.homeNotifications,
                    config = graph.homeConfig,
                    analytics = graph.homeAnalytics,
                    today = { LocalDate.now() },
                )
            }
        },
    )
    ShownEffect(shown, onShown = {
        viewModel.refresh()
        CommonMvp.dependencies.screenViewed(HomeScreenNames.WEEK_V2)
    })
    LocalBroadcastEffect(MvpBroadcasts.PREFERENCES_CHANGED, MvpBroadcasts.REMOTE_CONFIG_REFRESHED) { viewModel.refresh() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.loading, state.empty) {
        if (!state.loading) home.onWeekEmpty(state.empty)
    }
    val navigate = { destination: Destination -> graph.navigator.open(context, destination) }
    WeekTabScreen(
        viewModel = viewModel,
        bottomPadding = bottomPadding,
        actions = WeekTabActions(
            showStatistics = home.showStatistics,
            showSettings = home.showSettings,
            showStartWorkout = home.showStartMenu,
            open = { tap -> open(tap, navigate, home) },
            showPlan = { navigate(PlanDetails(it)) },
            showTemplate = { navigate(TemplateDetails(it, viewOnly = true)) },
            startPlanDay = { start ->
                when (start) {
                    is WeekTabPlanStart.Workout -> navigate(StartWorkoutConfirmation(start.routine, fromPlan = true))
                    WeekTabPlanStart.Finished -> navigate(PlanCongratulations)
                    WeekTabPlanStart.Nothing -> Unit
                }
            },
        ),
    )
}

private fun open(tap: HomeNotificationTap, navigate: (Destination) -> Unit, home: WeekTabHomeActions) {
    when (tap) {
        HomeNotificationTap.UpdateApp -> navigate(PlayStoreListing)
        is HomeNotificationTap.OpenUrl -> navigate(WebLink(tap.url))
        is HomeNotificationTap.Open -> when (tap.action) {
            HomeNotification.ActionType.MUSCLE_GOALS -> navigate(SetSuggestions)
            HomeNotification.ActionType.EXERCISES -> navigate(ExerciseLibrary)
            HomeNotification.ActionType.PLANS -> home.showPlans()
            HomeNotification.ActionType.SETTINGS -> home.showSettings()
            HomeNotification.ActionType.NONE, HomeNotification.ActionType.MAINTENANCE -> Unit
        }
        HomeNotificationTap.Nothing -> Unit
    }
}
