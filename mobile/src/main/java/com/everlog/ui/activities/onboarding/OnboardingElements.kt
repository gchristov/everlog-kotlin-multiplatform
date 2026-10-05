package com.everlog.ui.activities.onboarding

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.everlog.R
import com.everlog.ui.design.elements.AppBar
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
    const val Collapse = 250
    const val Enter = 350
    const val AppendDelay = 80
    const val Scroll = 300
    const val ProgressFill = 300
    val EnterOffset = 16.dp
}

// Colours the design uses that the theme doesn't have yet
@Immutable
internal data class OnboardingColors(
    val accentPressed: Color,
    val accentFillSelected: Color,
    val accentFillCurrent: Color,
    val textTertiary: Color,
)

@Composable
internal fun onboardingColors() = OnboardingColors(
    accentPressed = colorResource(R.color.main_accent_darker),
    accentFillSelected = colorResource(R.color.main_accent_faded_1),
    accentFillCurrent = colorResource(R.color.main_accent_faded_2),
    textTertiary = colorResource(R.color.gray_3),
)

internal object OnboardingType {
    val QuestionTitle = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp)
}

// Top bar: the app bar with Skip setup, and the progress segments under it
@Composable
internal fun OnboardingTopBar(
    answered: Int,
    total: Int,
    onSkipSetup: () -> Unit,
) {
    val progressDescription = if (answered < total) {
        stringResource(R.string.onboarding_step, answered + 1, total)
    } else {
        stringResource(R.string.onboarding_all_steps_answered, total)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.backgrounds.primary)
            // A band under the progress bar, so content scrolling up disappears below it
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppBar(
            actions = {
                // Text button padding plus the app bar's 4dp inset end the text on the 16dp margin,
                // in line with the progress bar
                AppTertiaryButton(
                    onClick = onSkipSetup,
                    text = stringResource(R.string.onboarding_skip_setup),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                )
            },
        )
        ProgressSegments(
            // In line with the content on tablets, while the app bar goes edge to edge
            modifier = Modifier
                .widthIn(max = LocalAppScreenMaxContentWidth.current)
                .semantics { contentDescription = progressDescription },
            // Every question is answered in order, so the next one follows the answered ones. Once
            // all are answered, every segment is filled and none is current.
            done = answered,
            current = answered,
            total = total,
        )
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
    val colors = onboardingColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(total) { index ->
            val fill by animateFloatAsState(
                targetValue = if (index < done) 1f else 0f,
                animationSpec = tween(OnboardingMotion.ProgressFill, easing = OnboardingMotion.Standard),
                label = "segmentFill",
            )
            val track by animateColorAsState(
                targetValue = if (index == current) colors.accentFillCurrent else Theme.backgrounds.separator,
                animationSpec = tween(OnboardingMotion.Select, easing = OnboardingMotion.Standard),
                label = "segmentTrack",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
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
    val colors = onboardingColors()
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(OnboardingMotion.Press, easing = OnboardingMotion.Standard),
        label = "press",
    )
    val border by animateColorAsState(
        targetValue = when {
            pressed -> colors.accentPressed
            selected -> Theme.contentColors.action
            else -> Color.Transparent
        },
        animationSpec = tween(OnboardingMotion.Select, easing = OnboardingMotion.Standard),
        label = "border",
    )
    val fill by animateColorAsState(
        targetValue = when {
            pressed -> Theme.backgrounds.separator
            selected -> colors.accentFillSelected
            else -> Theme.backgrounds.surface
        },
        animationSpec = tween(OnboardingMotion.Select, easing = OnboardingMotion.Standard),
        label = "fill",
    )
    val check by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(OnboardingMotion.Select, delayMillis = 40, easing = OnboardingMotion.EmphasizedDecelerate),
        label = "check",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clip(Theme.shapes.surface)
            .background(fill)
            // Transparent when unselected, so selecting never shifts the layout
            .border(1.5.dp, border, Theme.shapes.surface)
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
            .height(96.dp),
        selected = selected,
        onClick = onClick,
        description = "$title, $description",
    ) { check ->
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppText(text = title, style = TextStyle(fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium))
            AppText(text = description, style = Theme.typography.caption, color = Theme.contentColors.secondary)
        }
        AnimatedCheck(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(18.dp),
            progress = check,
            strokeWidth = 2.5f,
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
                    modifier = Modifier.size(20.dp),
                    progress = check,
                    strokeWidth = 2.5f,
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
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
        )
        AnimatedCheck(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(12.dp),
            progress = check,
            strokeWidth = 3f,
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
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            color = labelColor,
            maxLines = 1,
        )
    }
}

// Reminder time, opening a time picker
@Composable
internal fun TimeRow(
    time: String,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.onboarding_reminder_time_description, time)
    AppSurface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentPadding = PaddingValues(),
    ) {
        AppListItem(
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
    val colors = onboardingColors()
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
            titleColor = if (skipped) colors.textTertiary else Theme.contentColors.action,
            trailing = {
                AppIcon(modifier = Modifier.size(18.dp), imageVector = ImageVector.vectorResource(R.drawable.ic_edit), tint = colors.textTertiary)
            },
            onClick = onClick,
        )
    }
}

// The next question while another one is being edited: dim and outlined, not tappable
@Composable
internal fun UpNextRow(label: String) {
    val colors = onboardingColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Theme.backgrounds.separator, Theme.shapes.surface),
    ) {
        AppListItem(
            header = label,
            title = stringResource(R.string.onboarding_up_next),
            titleColor = colors.textTertiary,
        )
    }
}

// A check mark that draws in by trimming its stroke, in a 24-unit box
@Composable
internal fun AnimatedCheck(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 2.5f,
    color: Color = Theme.contentColors.action,
) {
    val path = remember {
        Path().apply {
            moveTo(5f, 12.5f)
            lineTo(9.5f, 17f)
            lineTo(19f, 7.5f)
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
