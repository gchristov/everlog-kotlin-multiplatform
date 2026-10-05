package com.everlog.ui.activities.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.everlog.ui.design.elements.AppSurface
import com.everlog.ui.design.elements.AppText
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
// to happen as a three-step timeline, then Let's go in the footer opens the questions
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
        // The card comes in with the first step, the later steps follow inside it
        EnterAnimation(animateIn = animateIn, delayMillis = WelcomeMotion.StepsDelay) {
            AppSurface(modifier = Modifier.fillMaxWidth()) {
                WelcomeSteps.forEachIndexed { index, step ->
                    EnterAnimation(animateIn = animateIn && index > 0, delayMillis = WelcomeMotion.StepStagger * index) {
                        TimelineStep(
                            title = stringResource(step.title),
                            description = stringResource(step.description),
                            current = index == 0,
                            last = index == WelcomeSteps.lastIndex,
                        )
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

// A step on the timeline: a dot joined to the next step's by a line, then the step's title and
// description. The current step's dot is filled.
@Composable
private fun TimelineStep(
    title: String,
    description: String,
    current: Boolean,
    last: Boolean,
) {
    Row(
        // So the line runs the full height of the step
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        Column(
            modifier = Modifier.width(TimelineSizes.Column),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val dot = Modifier
                .padding(top = TimelineSizes.DotTop)
                .size(TimelineSizes.Dot)
            Box(
                modifier = if (current) {
                    dot.background(Theme.contentColors.action, CircleShape)
                } else {
                    dot.border(TimelineSizes.Stroke, Theme.contentColors.secondary, CircleShape)
                }
            )
            if (!last) {
                Box(
                    modifier = Modifier
                        .padding(vertical = TimelineSizes.LineGap)
                        .weight(1f)
                        .width(TimelineSizes.Stroke)
                        .background(Theme.backgrounds.separator)
                )
            }
        }
        Column(
            modifier = Modifier.padding(bottom = if (last) 0.dp else TimelineSizes.StepGap),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
        ) {
            AppText(text = title, style = Theme.typography.subtitle)
            AppText(text = description, style = Theme.typography.caption, color = Theme.contentColors.secondary)
        }
    }
}

// Sizes from the design's timeline, which the design system doesn't have
private object TimelineSizes {
    val Column = 20.dp
    val Dot = 12.dp
    // Centres the dot on the title's first line
    val DotTop = 4.dp
    val Stroke = 1.5.dp
    val LineGap = 6.dp
    val StepGap = 24.dp
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
