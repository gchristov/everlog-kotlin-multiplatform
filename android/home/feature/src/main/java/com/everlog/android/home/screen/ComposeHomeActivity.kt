package com.everlog.android.home.screen

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.common.mvp.CommonMvp
import com.everlog.android.common.mvp.activity.SignedInComposeActivity
import com.everlog.android.common.navigation.Destination
import com.everlog.android.common.navigation.NewPlan
import com.everlog.android.common.navigation.ResumeWorkout
import com.everlog.android.common.navigation.StartEmptyWorkout
import com.everlog.android.common.navigation.StartWorkoutConfirmation
import com.everlog.android.common.navigation.TemplateEditor
import com.everlog.android.common.navigation.TemplatePicker
import com.everlog.android.home.HomeGraph
import com.everlog.android.home.HomeScreenNames
import kotlinx.coroutines.Dispatchers

// Home in Compose (HomeScreen), while Remote Config's home_v2 is on. The navigator opens it for Home.
class ComposeHomeActivity : SignedInComposeActivity() {
    private val graph by lazy { appGraph<HomeGraph>() }

    private val viewModel by viewModels<HomeViewModel> {
        viewModelFactory {
            initializer {
                HomeViewModel(
                    dispatcher = Dispatchers.Main,
                    savedState = createSavedStateHandle(),
                    services = graph.homeServices,
                    planProgress = graph.planProgress,
                    analytics = graph.homeAnalytics,
                )
            }
        }
    }

    private val updateFlow = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        appUpdates?.onFlowResult(result.resultCode)
    }
    private var appUpdates: HomeAppUpdates? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isOpened) return
        appUpdates = graph.homeServices.appUpdates(this, updateFlow, viewModel::onUpdateReady)
    }

    @Composable
    override fun Content() {
        // The template picked from the start menu opens on its start screen
        val pickTemplate = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            TemplatePicker.read(result.data)?.let { navigate(StartWorkoutConfirmation(it, fromPlan = false)) }
        }
        HomeScreen(
            viewModel = viewModel,
            sheetActions = HomeSheetActions(
                startFromTemplate = { pickTemplate.launch(graph.navigator.intent(this, TemplatePicker)) },
                startEmpty = { navigate(StartEmptyWorkout) },
                newTemplate = { navigate(TemplateEditor()) },
                newPlan = { navigate(NewPlan) },
                resume = { navigate(ResumeWorkout(it)) },
            ),
            onRestartForUpdate = { appUpdates?.restart() },
        )
    }

    override fun onResume() {
        super.onResume()
        if (!isOpened) return
        CommonMvp.dependencies.screenViewed(HomeScreenNames.HOME)
        when (val resume = viewModel.onResumed()) {
            is HomeResume.Reopen -> navigate(ResumeWorkout(resume.workout))
            HomeResume.CheckForUpdate -> appUpdates?.check()
            HomeResume.Nothing -> Unit
        }
    }

    override fun onPause() {
        if (isOpened) viewModel.onPaused()
        super.onPause()
    }

    override fun onDestroy() {
        appUpdates?.release()
        super.onDestroy()
    }

    private fun navigate(destination: Destination) = graph.navigator.open(this, destination)
}
