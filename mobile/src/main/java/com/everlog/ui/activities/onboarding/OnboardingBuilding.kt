package com.everlog.ui.activities.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimatable
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.everlog.R
import com.everlog.data.controllers.starterroutines.StarterProfile
import com.everlog.data.controllers.starterroutines.StarterWeek
import com.everlog.ui.activities.onboarding.OnboardingViewModel.Build
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

// The design's building animation (res/raw/everlog_building.json), one clip per marker
private object BuildingClips {
    // The barbell comes together, once
    const val Intro = "intro"
    // One lift and set down, whose last pose is its first, so it repeats seamlessly
    const val Loop = "loop"
    // The barbell goes, a ring and a check draw in
    const val Success = "success"
    // The plates turn grey and an alert draws in
    const val Error = "error"
}

private object BuildingMotion {
    // The check holds this long before the screen moves on
    const val ReadyHold = 300L
    // With animations off (or no animation), the screen still stays up for as long as the intro
    // and one loop
    const val ReducedMotionMinimum = 2200L
    const val TextFade = 150
    const val ActionsFade = 150
}

// The whole screen while the week builds, from the design's Building, Build ready and Build failed
// artboards. The animation always plays through: the intro, then at least one loop, more until the
// build is over, then the success or error clip. So the screen says the result only once the
// animation gets to it, and moves on once the check has drawn. After an error, Try again builds
// the week again from the intro, and Skip closes the screen.
@Composable
internal fun OnboardingBuilding(
    state: OnboardingViewModel.State,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    // The success clip has finished
    onShown: () -> Unit,
) {
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.everlog_building))
    val animatable = rememberLottieAnimatable()
    // What the screen says, which follows the animation rather than the build
    var shown by remember { mutableStateOf(Build.InProgress) }
    var showActions by remember { mutableStateOf(false) }

    val build by rememberUpdatedState(state.build)
    val currentOnShown by rememberUpdatedState(onShown)
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(compositionResult.isComplete, state.buildAttempt) {
        if (!compositionResult.isComplete) return@LaunchedEffect
        // Null if the animation couldn't load, so the screen carries on without it
        val composition = compositionResult.value
        shown = Build.InProgress
        showActions = false
        // Follows the system's animation scale, which Lottie doesn't by itself
        val scale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        val animate = composition != null && scale > 0f
        val result = if (animate) {
            animatable.play(composition, BuildingClips.Intro, speed = 1f / scale)
            do {
                animatable.play(composition, BuildingClips.Loop, speed = 1f / scale)
            } while (build == Build.InProgress)
            build
        } else {
            // The barbell holds still, for as long as the intro and a loop would take
            composition?.let { animatable.snapTo(it, progress = it.markerProgress(BuildingClips.Loop, end = false)) }
            delay(BuildingMotion.ReducedMotionMinimum)
            snapshotFlow { build }.first { it != Build.InProgress }
        }

        shown = result
        if (result == Build.Ready) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        if (composition != null) {
            if (animate) {
                animatable.play(composition, result.clip, speed = 1f / scale)
            } else {
                animatable.snapTo(composition, progress = composition.markerProgress(result.clip, end = true))
            }
        }
        when (result) {
            Build.Ready -> {
                delay(BuildingMotion.ReadyHold)
                currentOnShown()
            }
            Build.Failed -> showActions = true
            Build.InProgress -> Unit
        }
    }

    // Like Skip, once the failure shows
    BackHandler(enabled = showActions, onBack = onSkip)

    val actionsAlpha by animateFloatAsState(
        targetValue = if (showActions) 1f else 0f,
        animationSpec = tween(BuildingMotion.ActionsFade, easing = OnboardingMotion.Standard),
        label = "actions",
    )
    AppScreen(
        footer = {
            // Always laid out, so the content doesn't move when the actions appear
            AppFooter(
                modifier = Modifier
                    .alpha(actionsAlpha)
                    .then(if (showActions) Modifier else Modifier.clearAndSetSemantics {}),
                actions = listOf(
                    AppFooterAction(text = stringResource(R.string.onboarding_try_again), onClick = onRetry, enabled = showActions),
                    AppFooterAction(
                        text = stringResource(R.string.onboarding_skip),
                        onClick = onSkip,
                        enabled = showActions,
                        style = AppFooterAction.Style.Secondary,
                    ),
                ),
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Theme.spacing.large)
                .padding(bottom = contentPadding.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Decorative, the text says what's happening. Shrinks on short screens. Its space is
            // kept while it loads, so the text doesn't jump, but not if it couldn't load, so the
            // text is centred on its own.
            if (!compositionResult.isFailure) {
                LottieAnimation(
                    composition = compositionResult.value,
                    progress = { animatable.progress },
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = BuildingSizes.Animation)
                        .fillMaxWidth()
                        .aspectRatio(1f, matchHeightConstraintsFirst = true),
                )
                Spacer(Modifier.height(Theme.spacing.extraLarge))
            }
            BuildingText(shown = shown, answers = state.answers, routineCount = state.starter?.routines?.size ?: 0)
        }
    }
}

// Sizes from the design that the design system doesn't have
private object BuildingSizes {
    val Animation = 360.dp
}

private val Build.clip: String
    get() = if (this == Build.Ready) BuildingClips.Success else BuildingClips.Error

private suspend fun LottieAnimatable.play(composition: LottieComposition, marker: String, speed: Float) = animate(
    composition = composition,
    iteration = 1,
    iterations = 1,
    speed = speed,
    clipSpec = LottieClipSpec.Marker(marker),
)

// Where a marker's clip starts or ends, as LottieClipSpec.Marker plays it
private fun LottieComposition.markerProgress(marker: String, end: Boolean): Float {
    val clip = getMarker(marker) ?: return if (end) 1f else 0f
    val frame = if (end) clip.startFrame + clip.durationFrames else clip.startFrame
    return (frame / endFrame).coerceIn(0f, 1f)
}

@Composable
private fun BuildingText(
    shown: Build,
    answers: Map<String, Answer>,
    // Once the build is ready, for "Your routine is ready" or "Your routines are ready"
    routineCount: Int,
) {
    AnimatedContent(
        targetState = shown,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        transitionSpec = {
            fadeIn(tween(BuildingMotion.TextFade, easing = OnboardingMotion.Standard)) togetherWith
                    fadeOut(tween(BuildingMotion.TextFade, easing = OnboardingMotion.Standard))
        },
        contentAlignment = Alignment.TopCenter,
        label = "text",
    ) { build ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            AppText(
                text = when (build) {
                    Build.InProgress -> stringResource(R.string.onboarding_building)
                    Build.Ready -> pluralStringResource(R.plurals.onboarding_build_ready, routineCount)
                    Build.Failed -> stringResource(R.string.onboarding_build_failed)
                },
                style = Theme.typography.title,
                textAlign = TextAlign.Center,
            )
            AppText(
                text = if (build == Build.Failed) stringResource(R.string.onboarding_build_failed_body) else onboardingSummary(answers),
                color = Theme.contentColors.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// From the answers, e.g. "3 days · Gym · Build muscle", with the week's split after the days once
// it's built: "3 days · Push / Pull / Legs · Gym · Build muscle". Skipped questions count as their
// defaults, as for the build.
@Composable
internal fun onboardingSummary(answers: Map<String, Answer>, split: StarterWeek.Split? = null): String {
    val profile = OnboardingQuestions.starterProfile(answers)
    return listOfNotNull(
        pluralStringResource(R.plurals.onboarding_building_days, profile.daysPerWeek, profile.daysPerWeek),
        split?.label(),
        stringResource(
            when (profile.place) {
                StarterProfile.Place.GYM -> R.string.onboarding_where_gym
                StarterProfile.Place.HOME_DUMBBELLS -> R.string.onboarding_building_home
                StarterProfile.Place.BODYWEIGHT -> R.string.onboarding_where_bodyweight_answer
            }
        ),
        stringResource(
            when (profile.goal) {
                StarterProfile.Goal.GET_STRONGER -> R.string.onboarding_goal_stronger
                StarterProfile.Goal.BUILD_MUSCLE -> R.string.onboarding_goal_muscle
                StarterProfile.Goal.BUILD_ENDURANCE -> R.string.onboarding_goal_endurance
                StarterProfile.Goal.STAY_FIT -> R.string.onboarding_goal_fit
            }
        ),
    ).joinToString(stringResource(R.string.onboarding_building_separator))
}
