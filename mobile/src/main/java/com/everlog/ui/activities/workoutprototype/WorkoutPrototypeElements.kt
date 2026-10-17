package com.everlog.ui.activities.workoutprototype

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.everlog.R
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Pieces of the workout prototype the design system doesn't have yet. Kept local until we know
// which of them stay: the stepper, the Reps/Time toggle, the change chips and rest's amber.

internal object WorkoutColors {
    val Rest = Color(0xFFF2B84B)
    val RestMuted = Color(0xFF2A2210)
    val RestBorder = Color(0xFF6B5420)
    // The dock's insets (steppers, toggle) sit darker than the dock's tint
    val Inset = Color.Black.copy(alpha = 0.32f)
    // What the dock is editing: a done set
    val Editing = Color.White
}

internal object WorkoutSizes {
    val StepperCorner = 18.dp
    val StepButton = 38.dp
    val ToggleHeight = 28.dp
    val ChipHeight = 20.dp
    val ChipPadding = 7.dp
    val Node = 28.dp
    // The set nodes' tap target
    val NodeTarget = 48.dp
    // The spine's line, and the rings of the nodes on it
    val Line = 1.5.dp
    val SmallNode = 22.dp
    val Dot = 10.dp
    val ActiveDot = 8.dp
    // The design's outlined icon buttons: the workout's cog and each exercise's menu
    val OutlinedButton = 40.dp
    val RepeatDelayMillis = 400L
    val RepeatIntervalMillis = 70L
}

// Numbers in the dock: big and bold, figures the same width so they don't jiggle as they change
@Composable
internal fun numberStyle() = Theme.typography.heading.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")

// The small caps labels: SET 3 OF 4, THIS TIME
@Composable
internal fun tagStyle() = Theme.typography.small.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)

// − value +, with the label under the value. Holding a button keeps stepping; tapping the value
// asks for it instead.
@Composable
internal fun Stepper(
    value: String,
    label: String,
    onStep: (Int) -> Unit,
    onValueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(WorkoutSizes.StepperCorner)
    Row(
        modifier = modifier
            .clip(shape)
            .background(WorkoutColors.Inset)
            .border(1.dp, Theme.backgrounds.separator, shape)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(icon = ImageVector.vectorResource(R.drawable.ic_minus), contentDescription = "Less $label", onStep = { onStep(-1) })
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onValueClick),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppText(text = value, style = numberStyle(), maxLines = 1)
            AppText(text = label.uppercase(), style = tagStyle(), color = Theme.contentColors.secondary, maxLines = 1)
        }
        StepButton(icon = ImageVector.vectorResource(R.drawable.ic_add), contentDescription = "More $label", onStep = { onStep(1) })
    }
}

@Composable
private fun StepButton(icon: ImageVector, contentDescription: String, onStep: () -> Unit) {
    val step by rememberUpdatedState(onStep)
    val scope = rememberCoroutineScope()
    // The touch handling is our own (for holding to repeat), so it reports presses for the ripple
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(WorkoutSizes.StepButton)
            // Just the icon: the stepper's box already frames it. A round ripple shows the press.
            .clip(CircleShape)
            .indication(interactions, ripple())
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                onClick { step(); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val press = PressInteraction.Press(down.position)
                    interactions.tryEmit(press)
                    step()
                    val repeat = scope.launch {
                        delay(WorkoutSizes.RepeatDelayMillis)
                        while (true) {
                            step()
                            delay(WorkoutSizes.RepeatIntervalMillis)
                        }
                    }
                    val up = waitForUpOrCancellation()
                    repeat.cancel()
                    interactions.tryEmit(if (up != null) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(modifier = Modifier.size(18.dp), imageVector = icon)
    }
}

// Reps or Time for the set the dock shows
@Composable
internal fun ModeToggle(mode: SetMode, onModeChange: (SetMode) -> Unit, modifier: Modifier = Modifier) {
    val shape = Theme.shapes.button
    Row(
        modifier = modifier
            .clip(shape)
            .background(WorkoutColors.Inset)
            .border(1.dp, Theme.backgrounds.separator, shape)
            .padding(2.dp),
    ) {
        listOf(SetMode.Reps to "Reps", SetMode.Time to "Time").forEach { (option, text) ->
            val selected = option == mode
            Box(
                modifier = Modifier
                    .height(WorkoutSizes.ToggleHeight)
                    .clip(shape)
                    .background(if (selected) Theme.contentColors.action else Color.Transparent)
                    .clickable { onModeChange(option) }
                    .padding(horizontal = Theme.spacing.medium),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = text,
                    style = Theme.typography.small.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (selected) Theme.contentColors.onAction else Theme.contentColors.secondary,
                )
            }
        }
    }
}

// What changed since last time on a done set: green for more, coral for less
@Composable
internal fun ChangeChip(text: String, up: Boolean) {
    val color = if (up) Theme.contentColors.action else Theme.contentColors.destructive
    Box(
        modifier = Modifier
            .height(WorkoutSizes.ChipHeight)
            .clip(RoundedCornerShape(WorkoutSizes.ChipHeight / 2))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = WorkoutSizes.ChipPadding),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = text, style = Theme.typography.small.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold), color = color, maxLines = 1)
    }
}

// The chips for a done set compared with last time: reps or time first, then weight
internal fun changes(values: SetValues, last: SetValues?): List<Pair<String, Boolean>> {
    if (last == null || last.mode != values.mode) return emptyList()
    val chips = mutableListOf<Pair<String, Boolean>>()
    when (values.mode) {
        SetMode.Reps -> (values.reps - last.reps).takeIf { it != 0 }?.let {
            chips += "${signed(it)} ${if (kotlin.math.abs(it) == 1) "rep" else "reps"}" to (it > 0)
        }
        SetMode.Time -> (values.timeSeconds - last.timeSeconds).takeIf { it != 0 }?.let { chips += "${signed(it)}s" to (it > 0) }
    }
    (values.weightKg - last.weightKg).takeIf { kotlin.math.abs(it) >= 0.01f }?.let {
        chips += "${if (it > 0) "+" else "−"}${formatWeight(kotlin.math.abs(it))} kg" to (it > 0)
    }
    return chips
}

private fun signed(value: Int) = if (value > 0) "+$value" else "−${-value}"

internal fun SetValues.summary(): String = when (mode) {
    SetMode.Reps -> "$reps × ${formatWeight(weightKg)} kg"
    SetMode.Time -> "${timeSeconds}s × ${formatWeight(weightKg)} kg"
}

internal enum class NodeStyle {
    Upcoming,
    Current,
    Done,
    Editing,
}

// A set's node on the spine: a tick, grey until the set is done, then filled lime. The current set's
// is outlined in lime, the one being edited in white. Tapping it completes the set (or undoes it), in
// a 48dp target around the 28dp tick.
@Composable
internal fun SetNode(style: NodeStyle, onClick: (() -> Unit)?) {
    val action = Theme.contentColors.action
    val (background, border, content) = when (style) {
        NodeStyle.Done -> Triple(action, action, Theme.contentColors.onAction)
        NodeStyle.Current -> Triple(Theme.backgrounds.primary, action, action)
        NodeStyle.Upcoming -> Triple(Theme.backgrounds.primary, Theme.backgrounds.separator, Theme.contentColors.secondary)
        NodeStyle.Editing -> Triple(Theme.backgrounds.primary, WorkoutColors.Editing, WorkoutColors.Editing)
    }
    Box(
        modifier = Modifier
            .size(WorkoutSizes.NodeTarget)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(WorkoutSizes.Node)
                .clip(CircleShape)
                .background(background)
                .border(WorkoutSizes.Line, border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(modifier = Modifier.size(15.dp), imageVector = ImageVector.vectorResource(R.drawable.ic_check), tint = content)
        }
    }
}

// Add set's node: a dashed ring with a plus, lime once the line reaches it, otherwise grey like the
// upcoming sets' nodes
@Composable
internal fun AddNode(lit: Boolean) {
    val ring = if (lit) Theme.contentColors.action else Theme.backgrounds.separator
    Box(
        modifier = Modifier
            .size(WorkoutSizes.Node)
            .clip(CircleShape)
            .background(Theme.backgrounds.primary)
            .drawBehind {
                val stroke = WorkoutSizes.Line.toPx()
                drawRoundRect(
                    color = ring,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(size.width / 2),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            modifier = Modifier.size(15.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_add),
            tint = if (lit) Theme.contentColors.action else Theme.contentColors.secondary,
        )
    }
}

// An icon button (or AppOverflowMenu) in a thin circle
@Composable
internal fun Modifier.outlinedCircle(): Modifier = this
    .size(WorkoutSizes.OutlinedButton)
    .border(1.dp, Theme.backgrounds.separator, CircleShape)

// Rest's node, between the last done set and the next one
@Composable
internal fun RestNode() {
    Box(
        modifier = Modifier
            .size(WorkoutSizes.SmallNode)
            .clip(CircleShape)
            .background(WorkoutColors.RestMuted)
            .border(WorkoutSizes.Line, WorkoutColors.Rest, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(modifier = Modifier.size(12.dp), imageVector = ImageVector.vectorResource(R.drawable.ic_timer), tint = WorkoutColors.Rest)
    }
}

// A small dot: the start marker and each exercise's header
@Composable
internal fun SpineDot(color: Color, size: Dp = WorkoutSizes.Dot) {
    Box(modifier = Modifier.size(size).clip(CircleShape).background(color))
}

// A small pill for the rest and set timers' ±10s, in their own colour
// A pill in the dock's darker inset with a thin outline, like its cog: Add exercise and the timers'
// ±10s
@Composable
internal fun DockPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = WorkoutSizes.OutlinedButton,
    horizontalPadding: Dp = Theme.spacing.large,
) {
    val shape = Theme.shapes.button
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(WorkoutColors.Inset)
            .border(1.dp, Theme.backgrounds.separator, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = text, style = Theme.typography.button, maxLines = 1)
    }
}
