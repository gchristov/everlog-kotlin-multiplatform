package com.everlog.ui.activities.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.ui.design.elements.AppBarHeader
import com.everlog.ui.design.elements.list.AppListGroup
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme

private data class WelcomeStep(
    @StringRes val title: Int,
    @StringRes val description: Int,
)

private val WelcomeSteps = listOf(
    WelcomeStep(R.string.onboarding_welcome_step_questions, R.string.onboarding_welcome_step_questions_description),
    WelcomeStep(R.string.onboarding_welcome_step_week, R.string.onboarding_welcome_step_week_description),
    WelcomeStep(R.string.onboarding_welcome_step_yours, R.string.onboarding_welcome_step_yours_description),
)

// Entrance timings from the design's "welcome enters" timeline. Let's go fades in last, in the footer.
internal object WelcomeMotion {
    const val StepsDelay = 120
    const val StepStagger = 60
    const val ButtonDelay = 300
    const val ButtonFade = 200
}

// The first onboarding step: its header in the app bar (OnboardingWelcomeHeader), then what's about
// to happen as a list of three steps, then Let's go in the footer opens the questions
@Composable
internal fun OnboardingWelcome(
    scrollState: ScrollState,
    contentPadding: PaddingValues,
    // False when coming back to the screen (e.g. after rotating), so the entrance doesn't replay
    animateIn: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Theme.spacing.large)
            // The header's bottom padding plus this make the design's gap under the body
            .padding(top = Theme.spacing.large, bottom = contentPadding.calculateBottomPadding() + Theme.spacing.large),
    ) {
        // The group comes in with the first step, the later steps follow inside it
        EnterAnimation(animateIn = animateIn, delayMillis = WelcomeMotion.StepsDelay) {
            AppListGroup {
                WelcomeSteps.forEachIndexed { index, step ->
                    row {
                        EnterAnimation(animateIn = animateIn && index > 0, delayMillis = WelcomeMotion.StepStagger * index) {
                            AppListItem(
                                modifier = Modifier.semantics(mergeDescendants = true) {},
                                title = stringResource(step.title),
                                subtitle = stringResource(step.description),
                                leading = { StepMarker(current = index == 0) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// The welcome's header, for the app bar. Comes in first, before the timeline.
@Composable
internal fun OnboardingWelcomeHeader(animateIn: Boolean) {
    EnterAnimation(animateIn = animateIn, delayMillis = 0) {
        AppBarHeader(
            eyebrow = stringResource(R.string.onboarding_welcome_eyebrow),
            title = stringResource(R.string.onboarding_welcome_title),
            body = stringResource(R.string.onboarding_welcome_body),
        )
    }
}

// A step's marker, in a list item's leading slot: filled for the current step, outlined for the ones
// to come
@Composable
private fun StepMarker(current: Boolean) {
    Box(
        modifier = Modifier.size(StepMarkerSizes.Box),
        contentAlignment = Alignment.Center,
    ) {
        val dot = Modifier.size(StepMarkerSizes.Dot)
        Box(
            modifier = if (current) {
                dot.background(Theme.contentColors.action, CircleShape)
            } else {
                dot.border(StepMarkerSizes.Stroke, Theme.contentColors.secondary, CircleShape)
            }
        )
    }
}

// Sizes from the design's step markers, which the design system doesn't have
private object StepMarkerSizes {
    // The same box as a leading AppIcon, so the text lines up with other list items
    val Box = 24.dp
    val Dot = 12.dp
    val Stroke = 1.5.dp
}

// Fades its content in, sliding it up unless [slide] is off. Drawn in place from the start, so the
// layout never shifts as things come in.
@Composable
internal fun EnterAnimation(
    animateIn: Boolean,
    delayMillis: Int,
    slide: Boolean = true,
    durationMillis: Int = OnboardingMotion.Enter,
    easing: Easing = OnboardingMotion.EmphasizedDecelerate,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis, delayMillis, easing))
    }
    val offset = with(LocalDensity.current) { OnboardingMotion.EnterOffset.toPx() }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress.value
            if (slide) translationY = (1f - progress.value) * offset
        }
    ) {
        content()
    }
}
