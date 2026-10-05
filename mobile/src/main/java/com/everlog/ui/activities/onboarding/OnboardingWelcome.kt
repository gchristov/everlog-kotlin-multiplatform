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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
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
            // No dividers, so the markers' line runs through the rows unbroken
            AppListGroup(showDividers = false) {
                WelcomeSteps.forEachIndexed { index, step ->
                    row {
                        EnterAnimation(animateIn = animateIn && index > 0, delayMillis = WelcomeMotion.StepStagger * index) {
                            AppListItem(
                                modifier = Modifier.semantics(mergeDescendants = true) {},
                                title = stringResource(step.title),
                                subtitle = stringResource(step.description),
                                leading = {
                                    StepMarker(
                                        current = index == 0,
                                        first = index == 0,
                                        last = index == WelcomeSteps.lastIndex,
                                    )
                                },
                                leadingAlignment = Alignment.Top,
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
// to come, and joined to the markers above and below by a line. Use with the list item's leading
// content aligned to the top.
@Composable
private fun StepMarker(
    current: Boolean,
    first: Boolean,
    last: Boolean,
) {
    val lineColor = Theme.backgrounds.separator
    // One line of the list item's title (body style), so the dot sits on the title's first line
    val textMeasurer = rememberTextMeasurer()
    val titleStyle = Theme.typography.body
    val titleLineHeight = with(LocalDensity.current) {
        remember(titleStyle) { textMeasurer.measure("A", titleStyle).size.height.toDp() }
    }
    Box(
        modifier = Modifier
            .size(width = StepMarkerSizes.Width, height = titleLineHeight)
            .drawBehind {
                val stroke = StepMarkerSizes.Stroke.toPx()
                val gap = StepMarkerSizes.Dot.toPx() / 2 + StepMarkerSizes.LineGap.toPx()
                // Runs well past the marker: each row clips to its own bounds, so the line ends at
                // the row's edge and meets the next row's
                val reach = size.height * LineReach
                if (!first) {
                    drawLine(lineColor, Offset(center.x, center.y - gap), Offset(center.x, center.y - gap - reach), stroke)
                }
                if (!last) {
                    drawLine(lineColor, Offset(center.x, center.y + gap), Offset(center.x, center.y + gap + reach), stroke)
                }
            },
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
    // As wide as a leading AppIcon, so the text lines up with other list items
    val Width = 24.dp
    val Dot = 12.dp
    val Stroke = 1.5.dp
    // Between the dot and the line
    val LineGap = 6.dp
}

// How far the line reaches past the marker, in marker heights. More than any row is tall, even with
// large fonts.
private const val LineReach = 20f

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
