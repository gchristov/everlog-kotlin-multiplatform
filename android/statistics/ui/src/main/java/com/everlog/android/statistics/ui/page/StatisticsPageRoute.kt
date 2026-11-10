package com.everlog.android.statistics.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.compose.components.broadcast.LocalBroadcastEffect
import com.everlog.android.common.compose.components.shown.ShownEffect
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.common.mvp.CommonMvp
import com.everlog.android.common.mvp.MvpBroadcasts
import com.everlog.android.common.navigation.FeedbackForm
import com.everlog.android.common.navigation.FeedbackSource
import com.everlog.android.statistics.ui.StatisticsGraph
import com.everlog.android.statistics.ui.StatisticsScreenNames
import kotlinx.coroutines.Dispatchers
import org.threeten.bp.LocalDate

/**
 * The Activity tab's statistics page with what it needs from the app's graph. It reads its numbers again when it
 * shows ([shown]: its tab and page are the current ones), and when the user's settings or Remote Config change.
 */
@Composable
fun StatisticsPageRoute(shown: Boolean) {
    val context = LocalContext.current
    val graph = remember(context) { context.appGraph<StatisticsGraph>() }
    val viewModel: StatisticsPageViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                StatisticsPageViewModel(
                    dispatcher = Dispatchers.Main,
                    workouts = graph.workoutRepository,
                    settings = graph.userSettings,
                    analytics = graph.statisticsAnalytics,
                    preferences = graph.statisticsPreferences,
                    feedback = { graph.navigator.canOpen(Feedback) },
                    today = { LocalDate.now() },
                )
            }
        },
    )
    ShownEffect(shown, onShown = {
        viewModel.refresh()
        CommonMvp.dependencies.screenViewed(StatisticsScreenNames.STATISTICS_V2)
    })
    LocalBroadcastEffect(MvpBroadcasts.PREFERENCES_CHANGED, MvpBroadcasts.REMOTE_CONFIG_REFRESHED) { viewModel.refresh() }
    StatisticsPageScreen(viewModel = viewModel, onFeedback = { graph.navigator.open(context, Feedback) })
}

private val Feedback = FeedbackForm(FeedbackSource.ACTIVITY_STATISTICS)
