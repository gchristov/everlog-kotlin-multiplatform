package com.everlog.ui.activities.onboarding

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.R
import com.everlog.data.controllers.starterroutines.RealBuildStarterRoutinesUseCase
import com.everlog.data.repositories.RealExerciseRepository
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppDialog
import com.everlog.ui.design.elements.AppDialogAction
import com.everlog.ui.design.elements.AppLoadingScreen
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.rememberAppBarScrollBehavior
import com.everlog.ui.mvvm.createViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame

// Debug-only prototype of the onboarding (first run) journey from the Everlog Onboarding design,
// opened from Settings: the welcome, then the questions. Answers aren't saved. Build my week
// builds the starter routines, logs them and closes the screen. The building, reveal and end steps
// come later.
class OnboardingActivity : CommonComposeActivity() {
    private val viewModel by viewModels<OnboardingViewModel> {
        createViewModelFactory {
            OnboardingViewModel(
                dispatcher = Dispatchers.Main,
                buildStarterRoutinesUseCase = RealBuildStarterRoutinesUseCase(
                    dispatcher = Dispatchers.Default,
                    exerciseRepository = RealExerciseRepository(dispatcher = Dispatchers.IO),
                ),
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
        // Building can't be left part way
        if (!state.building) showSkipSetup = true
    }

    val context = LocalContext.current
    LaunchedEffect(state.finished) {
        if (state.finished) {
            if (state.buildFailed) Toast.makeText(context, R.string.onboarding_build_failed, Toast.LENGTH_LONG).show()
            onClose()
        }
    }

    // The welcome's entrance plays once, not again after e.g. rotating
    var welcomeShown by rememberSaveable { mutableStateOf(false) }
    val animateWelcomeIn = remember { !welcomeShown }
    LaunchedEffect(Unit) { welcomeShown = true }

    // The welcome's header scrolls away with its content, but only on screens too short to fit it
    val welcomeScrollState = rememberScrollState()
    val appBarScrollBehavior = rememberAppBarScrollBehavior(canScroll = { welcomeScrollState.canScrollForward })

    Box(modifier = Modifier.fillMaxSize()) {
        // One screen for every step, so the top bar and footer change in place around the content
        AppScreen(
            modifier = Modifier.nestedScroll(appBarScrollBehavior.nestedScrollConnection),
            topBar = {
                OnboardingTopBar(
                    welcome = state.welcome,
                    animateWelcomeIn = animateWelcomeIn,
                    answered = state.answers.size,
                    total = state.questions.size,
                    scrollBehavior = appBarScrollBehavior,
                    onSkipSetup = { showSkipSetup = true },
                )
            },
            footer = {
                OnboardingFooter(
                    state = state,
                    viewModel = viewModel,
                    animateIn = animateWelcomeIn,
                )
            },
        ) { contentPadding ->
            val enterOffset = with(LocalDensity.current) { OnboardingMotion.EnterOffset.roundToPx() }
            AnimatedContent(
                targetState = state.welcome,
                transitionSpec = {
                    // A fade through: the welcome lifts away, then the first question comes in like an
                    // appended one. The question waits for the welcome to go, as the two titles sit in
                    // the same place and overlapping them reads as a jumble.
                    val enterDelay = OnboardingMotion.WelcomeExit
                    (fadeIn(tween(OnboardingMotion.Enter, enterDelay, OnboardingMotion.EmphasizedDecelerate)) +
                            slideInVertically(tween(OnboardingMotion.Enter, enterDelay, OnboardingMotion.EmphasizedDecelerate)) { enterOffset }) togetherWith
                            (fadeOut(tween(OnboardingMotion.WelcomeExit, easing = OnboardingMotion.Standard)) +
                                    slideOutVertically(tween(OnboardingMotion.WelcomeExit, easing = OnboardingMotion.Standard)) { -enterOffset })
                },
                label = "step",
            ) { welcome ->
                if (welcome) {
                    OnboardingWelcome(
                        scrollState = welcomeScrollState,
                        contentPadding = contentPadding,
                        animateIn = animateWelcomeIn,
                    )
                } else {
                    OnboardingQuestionnaire(
                        state = state,
                        viewModel = viewModel,
                        contentPadding = contentPadding,
                    )
                }
            }
        }

        // Over everything, top bar and footer included, while the starter routines build
        AnimatedVisibility(
            visible = state.building,
            enter = fadeIn(tween(OnboardingMotion.Enter, easing = OnboardingMotion.Standard)),
            exit = fadeOut(tween(OnboardingMotion.Enter, easing = OnboardingMotion.Standard)),
        ) {
            AppLoadingScreen()
        }
    }

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
        title = stringResource(R.string.onboarding_skip_setup_title),
        body = stringResource(R.string.onboarding_skip_setup_body),
        onDismissRequest = onKeepGoing,
        primaryAction = AppDialogAction(text = stringResource(R.string.onboarding_keep_going), onClick = onKeepGoing),
        secondaryAction = AppDialogAction(text = stringResource(R.string.onboarding_skip_setup), onClick = onSkipSetup),
    )
}
