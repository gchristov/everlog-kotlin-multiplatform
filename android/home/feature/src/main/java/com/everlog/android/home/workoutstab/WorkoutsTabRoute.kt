package com.everlog.android.home.workoutstab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.compose.components.shown.ShownEffect
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.common.mvp.CommonMvp
import com.everlog.android.common.navigation.Destination
import com.everlog.android.common.navigation.ExerciseLibrary
import com.everlog.android.common.navigation.PlanDetails
import com.everlog.android.common.navigation.StartEmptyWorkout
import com.everlog.android.common.navigation.StartWorkout
import com.everlog.android.common.navigation.TemplateDetails
import com.everlog.android.common.navigation.TemplateEditor
import com.everlog.android.home.HomeGraph
import com.everlog.android.home.HomeScreenNames
import kotlinx.coroutines.Dispatchers
import org.threeten.bp.LocalDate

/**
 * Home's Workouts tab with what it needs from the app's graph. It reads the plan being followed again when it shows
 * ([shown]: its tab is the current one), and counts its card as seen only then.
 */
@Composable
fun WorkoutsTabRoute(shown: Boolean) {
    val context = LocalContext.current
    val graph = remember(context) { context.appGraph<HomeGraph>() }
    val viewModel: WorkoutsTabViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                WorkoutsTabViewModel(
                    dispatcher = Dispatchers.Main,
                    lists = graph.loadWorkoutsTabLists,
                    history = graph.loadTemplateHistory,
                    planProgress = graph.planProgress,
                    analytics = graph.homeAnalytics,
                    today = { LocalDate.now() },
                )
            }
        },
    )
    ShownEffect(
        shown,
        onShown = {
            viewModel.onShown()
            CommonMvp.dependencies.screenViewed(HomeScreenNames.WORKOUTS_V2)
        },
        onHidden = viewModel::onHidden,
    )
    val navigate = { destination: Destination -> graph.navigator.open(context, destination) }
    WorkoutsTabScreen(
        viewModel = viewModel,
        actions = WorkoutsTabActions(
            showExercises = { navigate(ExerciseLibrary) },
            showTemplate = { navigate(TemplateDetails(it, viewOnly = false)) },
            start = { navigate(StartWorkout(it.routine, it.fromPlan)) },
            startEmpty = { navigate(StartEmptyWorkout) },
            newTemplate = { navigate(TemplateEditor()) },
            showPlan = { navigate(PlanDetails(it)) },
        ),
    )
}
