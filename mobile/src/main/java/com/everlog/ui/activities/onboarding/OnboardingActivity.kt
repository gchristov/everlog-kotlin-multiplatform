package com.everlog.ui.activities.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.R
import com.everlog.data.controllers.starterroutines.RealBuildStarterRoutinesUseCase
import com.everlog.data.controllers.starterroutines.RealSaveStarterRoutinesUseCase
import com.everlog.data.repositories.RealExerciseRepository
import com.everlog.data.repositories.RealRoutineRepository
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppBarScrollBehavior
import com.everlog.ui.design.elements.AppDialog
import com.everlog.ui.design.elements.AppDialogAction
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.rememberAppBarScrollBehavior
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.mvvm.createViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame

// The onboarding (first run) journey from the Everlog Onboarding design, for a signed in user: the
// welcome, then the questions. The units and days a week answers are saved to Settings as soon as
// they're given. Build my templates builds the starter routines behind the building animation, then
// the reveal shows them. Looks good saves them behind the same animation and closes the screen.
// Build my own template opens the routine builder, and the screen closes once it saves a routine.
// If building or saving fails, the user can try again or skip.
//
// The screen doesn't know what comes after it. It closes with RESULT_OK whether the user set up
// or skipped, and the screen that opened it for a result decides what's next.
class OnboardingActivity : CommonComposeActivity() {
    private val viewModel by viewModels<OnboardingViewModel> {
        createViewModelFactory {
            OnboardingViewModel(
                dispatcher = Dispatchers.Main,
                buildStarterRoutinesUseCase = RealBuildStarterRoutinesUseCase(
                    dispatcher = Dispatchers.Default,
                    exerciseRepository = RealExerciseRepository(dispatcher = Dispatchers.IO),
                ),
                saveStarterRoutinesUseCase = RealSaveStarterRoutinesUseCase(
                    dispatcher = Dispatchers.Default,
                    routineRepository = RealRoutineRepository(dispatcher = Dispatchers.IO),
                ),
                settings = RealOnboardingSettings(this),
                firstDayOfWeek = SettingsManager.manager.firstDayOfWeek(),
            )
        }
    }

    // Only a saved routine counts. Backing out of the builder leaves the reveal as it was.
    private val routineBuilder = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) viewModel.onOwnRoutineSaved()
    }

    @Composable
    override fun Content() = OnboardingScreen(
        viewModel = viewModel,
        onOpenRoutineBuilder = {
            // Not the routine's details after saving: the screen decides what comes next
            routineBuilder.launch(CreateRoutineActivity.launchIntent(this, CreateRoutineActivity.Companion.Properties().showDetailsOnSuccess(false)))
        },
        onClose = {
            setResult(RESULT_OK)
            finish()
        },
    )
}

@Composable
internal fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOpenRoutineBuilder: () -> Unit,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSkipSetup by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = !state.finished) {
        // Building and saving can't be left part way. Once either fails, OnboardingProgress
        // handles Back. The reveal ignores Back, as the answers are in and the routines are built.
        if (state.step == OnboardingViewModel.Step.Welcome || state.step == OnboardingViewModel.Step.Questions) showSkipSetup = true
    }

    LaunchedEffect(state.finished) {
        if (state.finished) onClose()
    }

    LaunchedEffect(state.openRoutineBuilder) {
        if (state.openRoutineBuilder) {
            onOpenRoutineBuilder()
            viewModel.onRoutineBuilderOpened()
        }
    }

    // The welcome's entrance plays once, not again after e.g. rotating
    var welcomeShown by rememberSaveable { mutableStateOf(false) }
    val animateWelcomeIn = remember { !welcomeShown }
    LaunchedEffect(Unit) { welcomeShown = true }

    // The welcome's header scrolls away with its content, but only on screens too short to fit it
    val welcomeScrollState = rememberScrollState()
    val appBarScrollBehavior = rememberAppBarScrollBehavior(canScroll = { welcomeScrollState.canScrollForward })

    // Every step of the screen. The welcome and the questions are one screen, whose top bar and footer
    // change in place around the content, so going between them isn't a change of screen here.
    AnimatedContent(
        targetState = state.step,
        // Mid crossfade both steps are see-through, so without this the window shows through as a grey flash
        modifier = Modifier.background(Theme.backgrounds.primary),
        contentKey = { step ->
            when (step) {
                OnboardingViewModel.Step.Welcome, OnboardingViewModel.Step.Questions -> OnboardingViewModel.Step.Questions
                else -> step
            }
        },
        transitionSpec = {
            fadeIn(tween(OnboardingMotion.Enter, easing = OnboardingMotion.Standard)) togetherWith
                    fadeOut(tween(OnboardingMotion.Enter, easing = OnboardingMotion.Standard))
        },
        label = "screen",
    ) { step ->
        when (step) {
            OnboardingViewModel.Step.Welcome, OnboardingViewModel.Step.Questions -> OnboardingSetup(
                state = state,
                viewModel = viewModel,
                animateWelcomeIn = animateWelcomeIn,
                welcomeScrollState = welcomeScrollState,
                appBarScrollBehavior = appBarScrollBehavior,
                onSkipSetup = { showSkipSetup = true },
            )
            OnboardingViewModel.Step.Building -> OnboardingBuilding(
                state = state,
                onRetry = viewModel::onRetryBuild,
                onSkip = viewModel::onSkipBuild,
                onShown = viewModel::onBuildShown,
            )
            OnboardingViewModel.Step.Reveal -> OnboardingReveal(
                state = state,
                onRoutineToggle = viewModel::onRoutineToggle,
                onLooksGood = viewModel::onLooksGood,
                onBuildOwn = viewModel::onBuildOwn,
            )
            OnboardingViewModel.Step.Saving -> OnboardingSaving(
                state = state,
                onRetry = viewModel::onRetrySave,
                onSkip = viewModel::onSkipSave,
                onShown = viewModel::onSaveShown,
            )
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

// The welcome, then the questions, on one AppScreen
@Composable
private fun OnboardingSetup(
    state: OnboardingViewModel.State,
    viewModel: OnboardingViewModel,
    animateWelcomeIn: Boolean,
    welcomeScrollState: ScrollState,
    appBarScrollBehavior: AppBarScrollBehavior,
    onSkipSetup: () -> Unit,
) {
    val welcome = state.step == OnboardingViewModel.Step.Welcome
    AppScreen(
        modifier = Modifier.nestedScroll(appBarScrollBehavior.nestedScrollConnection),
        topBar = {
            OnboardingTopBar(
                welcome = welcome,
                animateWelcomeIn = animateWelcomeIn,
                answered = state.answers.size,
                total = state.questions.size,
                scrollBehavior = appBarScrollBehavior,
                onSkipSetup = onSkipSetup,
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
            targetState = welcome,
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
            label = "welcome",
        ) { showWelcome ->
            if (showWelcome) {
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
