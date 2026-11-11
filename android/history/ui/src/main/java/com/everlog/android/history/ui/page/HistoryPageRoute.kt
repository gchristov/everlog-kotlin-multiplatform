package com.everlog.android.history.ui.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.compose.components.broadcast.LocalBroadcastEffect
import com.everlog.android.common.compose.components.shown.ShownEffect
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.common.mvp.CommonMvp
import com.everlog.android.common.mvp.MvpBroadcasts
import com.everlog.android.common.navigation.WorkoutDetails
import com.everlog.android.history.ui.HistoryGraph
import com.everlog.android.history.ui.HistoryScreenNames
import com.everlog.android.history.ui.R
import kotlinx.coroutines.Dispatchers
import org.threeten.bp.LocalDate

/**
 * The Activity tab's history page with what it needs from the app's graph. It reads again when it shows ([shown]:
 * its page is the current one in the tab being shown) and when the user's settings change. [tabShown] says whether
 * the Activity tab is the current one: once another tab is, the history's pages can go.
 */
@Composable
fun HistoryPageRoute(shown: Boolean, tabShown: Boolean, bottomPadding: Dp) {
    val context = LocalContext.current
    val graph = remember(context) { context.appGraph<HistoryGraph>() }
    val viewModel: HistoryPageViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HistoryPageViewModel(
                    dispatcher = Dispatchers.Main,
                    workouts = graph.workoutRepository,
                    settings = graph.userSettings,
                    preferences = graph.historyPreferences,
                    exampleNames = ExampleNames.map(context::getString),
                    today = { LocalDate.now() },
                )
            }
        },
    )
    ShownEffect(shown, onShown = {
        viewModel.refresh()
        CommonMvp.dependencies.screenViewed(HistoryScreenNames.HISTORY_V2)
    })
    // Only on moving to another tab, not on the app going to the back
    var wasTabShown by remember { mutableStateOf(tabShown) }
    LaunchedEffect(tabShown) {
        if (wasTabShown && !tabShown) viewModel.onLeft()
        wasTabShown = tabShown
    }
    LocalBroadcastEffect(MvpBroadcasts.PREFERENCES_CHANGED) { viewModel.refresh() }
    HistoryPageScreen(
        viewModel = viewModel,
        onWorkout = { workout -> graph.navigator.open(context, WorkoutDetails(workout, justFinished = false, fromPlan = false)) },
        bottomPadding = bottomPadding,
    )
}

private val ExampleNames = listOf(R.string.history_example_legs, R.string.history_example_push, R.string.history_example_pull)
