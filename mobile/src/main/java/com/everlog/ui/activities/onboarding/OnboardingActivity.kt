package com.everlog.ui.activities.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppDialog
import com.everlog.ui.design.elements.AppDialogAction
import com.everlog.ui.mvvm.createViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame

// Debug-only prototype of the onboarding (first run) journey from the Everlog Onboarding design,
// opened from Settings. UI only: answers aren't saved, nothing is logged, and Build my routine
// closes the screen. The building, reveal and end steps come later.
class OnboardingActivity : CommonComposeActivity() {
    private val viewModel by viewModels<OnboardingViewModel> {
        createViewModelFactory {
            OnboardingViewModel(
                dispatcher = Dispatchers.Main,
                firstDayOfWeek = SettingsManager.manager.firstDayOfWeek(),
            )
        }
    }

    @Composable
    override fun Content() = OnboardingScreen(
        viewModel = viewModel,
        onClose = { finish() },
    )
}

@Composable
internal fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSkipSetup by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = !state.finished) {
        showSkipSetup = true
    }

    LaunchedEffect(state.finished) {
        if (state.finished) onClose()
    }

    // The one screen for now. An intro before the questions will join it here, animating between
    // the two.
    OnboardingQuestionnaire(
        state = state,
        viewModel = viewModel,
        onSkipSetup = { showSkipSetup = true },
    )

    var skippingSetup by remember { mutableStateOf(false) }
    if (showSkipSetup) {
        SkipSetupDialog(
            onKeepGoing = { showSkipSetup = false },
            onSkipSetup = {
                showSkipSetup = false
                skippingSetup = true
            },
        )
    }
    // Close only once the dialog has gone. Its dim covers the whole task, so closing with it open
    // leaves the screen underneath dimmed until the close animation ends.
    if (skippingSetup) {
        LaunchedEffect(Unit) {
            awaitFrame()
            onClose()
        }
    }
}

@Composable
private fun SkipSetupDialog(
    onKeepGoing: () -> Unit,
    onSkipSetup: () -> Unit,
) {
    AppDialog(
        title = "Skip setup?",
        body = "We'll start you with an empty app. You can build routines yourself, or set this up later from Settings.",
        onDismissRequest = onKeepGoing,
        primaryAction = AppDialogAction(text = "Keep going", onClick = onKeepGoing),
        secondaryAction = AppDialogAction(text = "Skip setup", onClick = onSkipSetup),
    )
}
