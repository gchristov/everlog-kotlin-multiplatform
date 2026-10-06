package com.everlog.ui.activities.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.ui.design.elements.AppBar
import com.everlog.ui.design.elements.AppBarScrollBehavior
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppSurface
import com.everlog.ui.design.elements.AppTertiaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.LocalAppScreenMaxContentWidth
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme

// Pieces of the onboarding design that aren't in the design system yet. They stay local to this
// screen until we decide which ones to promote.

// Motion tokens from the design's motion notes
internal object OnboardingMotion {
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val Select = 150
    const val Press = 80
    const val PressScale = 0.98f
    // The check draws in just after the selection fill starts
    const val CheckDelay = 40
    const val Collapse = 250
    // A reopened question's options fade in as it expands
    const val Expand = 200
    const val ExpandDelay = 50
    // A collapsing question's summary row fades in once the options have gone
    const val SummaryFade = 150
    const val SummaryDelay = 100
    const val FadeOut = 120
    const val Enter = 350
    const val AppendDelay = 80
    const val Exit = 150
    const val Scroll = 300
    const val ProgressFill = 300
    // Let's go: the welcome leaves, then Skip setup, the progress bar and the first question come in
    const val WelcomeExit = 150
    const val TopBarFade = 200
    val EnterOffset = 16.dp
}

// Sizes from the design that the design system doesn't have
private object OnboardingSizes {
    val ProgressHeight = 3.dp
    val ProgressCornerRadius = 2.dp
    // A touch heavier than the outlined buttons' 1dp, so a selected option stands out
    val SelectionBorder = 1.5.dp
    val UnitCardHeight = 96.dp
    val UnitCardCheck = 18.dp
    // The same box as a trailing AppIcon, e.g. the pencil on summary rows
    val ListItemCheck = 24.dp
    val ChipCheck = 12.dp
    const val CheckStroke = 2.5f
    // Heavier on the small chip check so it stays legible
    const val ChipCheckStroke = 3f
}

// Top bar. On the welcome: an empty row with the welcome's header under it, scrolling away with the
// content. On the questions: Skip setup, and the progress segments under the row. Leaving the
// welcome, the header fades out, then the bar swaps to the questions' in one frame while the content
// is invisible, so nothing is seen to move.
@Composable
internal fun OnboardingTopBar(
    welcome: Boolean,
    // The welcome's entrance, the first time it shows
    animateWelcomeIn: Boolean,
    answered: Int,
    total: Int,
    scrollBehavior: AppBarScrollBehavior,
    onSkipSetup: () -> Unit,
) {
    val progressDescription = if (answered < total) {
        stringResource(R.string.onboarding_step, answered + 1, total)
    } else {
        stringResource(R.string.onboarding_all_steps_answered, total)
    }
    val questionsIn = fadeIn(tween(OnboardingMotion.TopBarFade, OnboardingMotion.WelcomeExit, OnboardingMotion.Standard))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.backgrounds.primary),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppBar(
            actions = {
                AnimatedVisibility(
                    visible = !welcome,
                    enter = questionsIn,
                    exit = fadeOut(tween(OnboardingMotion.WelcomeExit)),
                ) {
                    // Text button padding plus the app bar's 4dp inset end the text on the 16dp
                    // margin, in line with the progress bar
                    AppTertiaryButton(
                        onClick = onSkipSetup,
                        text = stringResource(R.string.onboarding_skip_setup),
                        contentPadding = PaddingValues(horizontal = Theme.spacing.medium),
                    )
                }
            },
            header = {
                AnimatedVisibility(
                    visible = welcome,
                    // Leaves with the welcome's content, then gives up its space at once
                    exit = fadeOut(tween(OnboardingMotion.WelcomeExit, easing = OnboardingMotion.Standard)) +
                            shrinkVertically(snap(OnboardingMotion.WelcomeExit)),
                ) {
                    OnboardingWelcomeHeader(animateIn = animateWelcomeIn)
                }
            },
            scrollBehavior = scrollBehavior,
        )
        AnimatedVisibility(
            visible = !welcome,
            // Takes its space as the header gives up its own, then fades in
            enter = expandVertically(snap(OnboardingMotion.WelcomeExit)) + questionsIn,
        ) {
            ProgressSegments(
                // In line with the content on tablets, while the app bar goes edge to edge. The band
                // under it hides content scrolling up.
                modifier = Modifier
                    .widthIn(max = LocalAppScreenMaxContentWidth.current)
                    .padding(bottom = Theme.spacing.small)
                    .semantics { contentDescription = progressDescription },
                // Every question is answered in order, so the next one follows the answered ones.
                // Once all are answered, every segment is filled and none is current.
                done = answered,
                current = answered,
                total = total,
            )
        }
    }
}

// Done segments fill from the left, the current one is a dimmer accent, upcoming ones the track
@Composable
private fun ProgressSegments(
    done: Int,
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.large),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        repeat(total) { index ->
            val fill by animateFloatAsState(
                targetValue = if (index < done) 1f else 0f,
                animationSpec = tween(OnboardingMotion.ProgressFill, easing = OnboardingMotion.Standard),
                label = "segmentFill",
            )
            val track by animateColorAsState(
                targetValue = if (index == current) Theme.backgrounds.actionMuted else Theme.backgrounds.separator,
                animationSpec = tween(OnboardingMotion.Select, easing = OnboardingMotion.Standard),
                label = "segmentTrack",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(OnboardingSizes.ProgressHeight)
                    .clip(RoundedCornerShape(OnboardingSizes.ProgressCornerRadius))
                    .background(track)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .background(Theme.contentColors.action)
                )
            }
        }
    }
}

// Shared selection behaviour for answer cards and chips: border and fill crossfade, press scale,
// a light haptic tick on selection
@Composable
private fun SelectableBox(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String,
    content: @Composable BoxScope.(checkProgress: Float) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) OnboardingMotion.PressScale else 1f,
        animationSpec = tween(OnboardingMotion.Press, easing = OnboardingMotion.Standard),
        label = "press",
    )
    val border by animateColorAsState(
        targetValue = when {
            pressed -> Theme.contentColors.actionPressed
            selected -> Theme.contentColors.action
            else -> Color.Transparent
        },
        animationSpec = tween(OnboardingMotion.Select, easing = OnboardingMotion.Standard),
        label = "border",
    )
    val fill by animateColorAsState(
        targetValue = when {
            pressed -> Theme.backgrounds.separator
            selected -> Theme.backgrounds.selected
            else -> Theme.backgrounds.surface
        },
        animationSpec = tween(OnboardingMotion.Select, easing = OnboardingMotion.Standard),
        label = "fill",
    )
    val check by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(OnboardingMotion.Select, delayMillis = OnboardingMotion.CheckDelay, easing = OnboardingMotion.EmphasizedDecelerate),
        label = "check",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clip(Theme.shapes.surface)
            .background(fill)
            // Transparent when unselected, so selecting never shifts the layout
            .border(OnboardingSizes.SelectionBorder, border, Theme.shapes.surface)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
            ) {
                if (!selected) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
            .semantics {
                contentDescription = description
                this.selected = selected
            },
    ) {
        content(check)
    }
}

// Units card: a big title over its description, with the check top right when selected
@Composable
internal fun UnitCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableBox(
        modifier = modifier
            .fillMaxWidth()
            .height(OnboardingSizes.UnitCardHeight),
        selected = selected,
        onClick = onClick,
        description = "$title, $description",
    ) { check ->
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = Theme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
        ) {
            AppText(text = title, style = Theme.typography.title)
            AppText(text = description, style = Theme.typography.caption, color = Theme.contentColors.secondary)
        }
        AnimatedCheck(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Theme.spacing.medium)
                .size(OnboardingSizes.UnitCardCheck),
            progress = check,
            strokeWidth = OnboardingSizes.CheckStroke,
        )
    }
}

// An answer as a list item, with the onboarding's selection outline and fill around it and the
// check in its trailing slot. The selection styling stays here, not in the design system.
@Composable
internal fun SelectableListItem(
    title: String,
    description: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SelectableBox(
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        onClick = onClick,
        description = listOfNotNull(title, description).joinToString(", "),
    ) { check ->
        AppListItem(
            title = title,
            subtitle = description,
            trailing = {
                AnimatedCheck(
                    modifier = Modifier.size(OnboardingSizes.ListItemCheck),
                    progress = check,
                    strokeWidth = OnboardingSizes.CheckStroke,
                )
            },
        )
    }
}

// Number chip (days a week): a square with a 22sp numeral, and a 12dp check top right when selected
@Composable
internal fun NumberChip(
    value: Int,
    // Read out instead of the number, e.g. "4 days a week"
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectableBox(
        modifier = modifier.aspectRatio(1f),
        selected = selected,
        onClick = onClick,
        description = description,
    ) { check ->
        AppText(
            modifier = Modifier.align(Alignment.Center),
            text = value.toString(),
            style = Theme.typography.heading,
        )
        AnimatedCheck(
            modifier = Modifier
                .align(Alignment.TopEnd)
                // The smaller inset keeps the check clear of the number on narrow chips
                .padding(Theme.spacing.extraSmall)
                .size(OnboardingSizes.ChipCheck),
            progress = check,
            strokeWidth = OnboardingSizes.ChipCheckStroke,
        )
    }
}

// Day-of-week chip, a square like the number chips. Multi-select, so the state is the outline, fill
// and an accent label, no check.
@Composable
internal fun DayChip(
    label: String,
    fullLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labelColor by animateColorAsState(
        targetValue = if (selected) Theme.contentColors.action else Theme.contentColors.secondary,
        animationSpec = tween(OnboardingMotion.Select, easing = OnboardingMotion.Standard),
        label = "dayLabel",
    )
    SelectableBox(
        modifier = modifier.aspectRatio(1f),
        selected = selected,
        onClick = onClick,
        description = fullLabel,
    ) {
        AppText(
            modifier = Modifier.align(Alignment.Center),
            text = label,
            style = Theme.typography.caption,
            color = labelColor,
            maxLines = 1,
        )
    }
}

// Reminder time, opening a time picker. A row for an AppListGroup.
@Composable
internal fun TimeRow(
    time: String,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.onboarding_reminder_time_description, time)
    AppListItem(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        title = stringResource(R.string.time),
        leading = {
            AppIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_time), tint = Theme.contentColors.secondary)
        },
        trailing = {
            AppText(text = time, style = Theme.typography.heading)
        },
        onClick = onClick,
    )
}

// Collapsed answer: the question's label over the answer in accent, with a pencil. The whole row
// edits it. Skipped answers are grey so they never read as a choice.
@Composable
internal fun SummaryRow(
    label: String,
    answer: String,
    skipped: Boolean,
    onClick: () -> Unit,
) {
    val shownAnswer = if (skipped) stringResource(R.string.onboarding_skipped) else answer
    val description = stringResource(R.string.onboarding_edit_answer, label, shownAnswer)
    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentPadding = PaddingValues(),
    ) {
        AppListItem(
            header = label,
            title = shownAnswer,
            titleColor = if (skipped) Theme.contentColors.secondary else Theme.contentColors.action,
            trailing = {
                AppIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_edit), tint = Theme.contentColors.secondary)
            },
            onClick = onClick,
        )
    }
}

// The next question while another one is being edited: dim and outlined, not tappable
@Composable
internal fun UpNextRow(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Theme.backgrounds.separator, Theme.shapes.surface),
    ) {
        AppListItem(
            header = label,
            title = stringResource(R.string.onboarding_up_next),
            titleColor = Theme.contentColors.secondary,
        )
    }
}

// A check mark that draws in by trimming its stroke, in a 24-unit box
@Composable
internal fun AnimatedCheck(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Float = OnboardingSizes.CheckStroke,
    color: Color = Theme.contentColors.action,
) {
    val path = remember {
        Path().apply {
            // Spans most of the box, about as wide as the app's ic_check
            moveTo(3.5f, 12.5f)
            lineTo(9f, 18f)
            lineTo(20.5f, 6.5f)
        }
    }
    TrimmedStroke(modifier = modifier, paths = listOf(path), progress = progress, strokeWidth = strokeWidth, color = color)
}

@Composable
private fun TrimmedStroke(
    paths: List<Path>,
    progress: Float,
    strokeWidth: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (progress <= 0f) return@Canvas
        scale(scale = size.width / 24f, pivot = Offset.Zero) {
            paths.forEach { path ->
                val measure = PathMeasure().apply { setPath(path, false) }
                val segment = Path()
                measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), segment, true)
                drawPath(
                    path = segment,
                    color = color,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

@Composable
internal fun VerticalSpace(height: Dp) {
    Spacer(modifier = Modifier.height(height))
}
