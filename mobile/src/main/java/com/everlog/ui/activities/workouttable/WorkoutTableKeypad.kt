package com.everlog.ui.activities.workouttable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.ui.activities.workoutprototype.DockPill
import com.everlog.ui.activities.workoutprototype.ModeToggle
import com.everlog.ui.activities.workoutprototype.SetMode
import com.everlog.ui.activities.workoutprototype.SetRef
import com.everlog.ui.activities.workoutprototype.ValueField
import com.everlog.ui.activities.workoutprototype.WorkoutColors
import com.everlog.ui.activities.workoutprototype.WorkoutPrototypeState
import com.everlog.ui.activities.workoutprototype.WorkoutPrototypeViewModel
import com.everlog.ui.activities.workoutprototype.ceilSeconds
import com.everlog.ui.activities.workoutprototype.dockBlur
import com.everlog.ui.activities.workoutprototype.formatClock
import com.everlog.ui.activities.workoutprototype.numberStyle
import com.everlog.ui.activities.workoutprototype.set
import com.everlog.ui.activities.workoutprototype.tagStyle
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme

internal class KeypadKeys(
    val onDigit: (Char) -> Unit,
    val onBackspace: () -> Unit,
    val onStep: (Int) -> Unit,
    val onNext: () -> Unit,
    val onComplete: () -> Unit,
    val onMode: (SetMode) -> Unit,
    val onHide: () -> Unit,
)

private object KeypadSizes {
    val Corner = 30.dp
    val Padding = 14.dp
    val Key = 48.dp
    val KeyCorner = 14.dp
    val Gap = 6.dp
    val StripHeight = 50.dp
    val StripCorner = 22.dp
}

/**
 * Docked at the bottom like the Spine and Dock prototype's dock: a floating card over the blurred
 * table. A running set timer or rest shows at its top. Once a cell is tapped the keypad opens and
 * stays open (until ⌄ hides it) as focus moves from cell to cell. Without it, Add exercise.
 */
@Composable
internal fun TableFooter(state: WorkoutPrototypeState, viewModel: WorkoutPrototypeViewModel, focus: CellFocus?, keys: KeypadKeys) {
    val shape = RoundedCornerShape(KeypadSizes.Corner)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = Theme.spacing.large, end = Theme.spacing.large, bottom = Theme.spacing.small + 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .dockBlur()
                .border(1.dp, Theme.backgrounds.separator, shape)
                .padding(KeypadSizes.Padding),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            val timer = state.setTimer
            val rest = state.rest
            when {
                timer != null -> TimerStrip(
                    label = "SET TIMER",
                    remainingSeconds = ceilSeconds(timer.remainingMillis(state.now)),
                    color = Theme.contentColors.action,
                    background = Theme.backgrounds.selected,
                    border = Theme.backgrounds.actionMuted,
                    onStep = { viewModel.onTimerStep(it * WorkoutPrototypeViewModel.TimerStepSeconds) },
                    endText = "Done",
                    onEnd = viewModel::onFinishTimerEarly,
                )
                rest != null -> TimerStrip(
                    label = "REST",
                    remainingSeconds = state.restRemainingSeconds(),
                    color = WorkoutColors.Rest,
                    background = WorkoutColors.RestMuted,
                    border = WorkoutColors.RestBorder,
                    onStep = { viewModel.onRestStep(it * WorkoutPrototypeViewModel.RestStepSeconds) },
                    endText = "Skip",
                    onEnd = viewModel::onSkipRest,
                )
            }
            AnimatedVisibility(
                visible = focus != null,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                // Keeps the last focus while it hides
                focus?.let { Keypad(state, it, keys) }
            }
            if (focus == null) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    DockPill(text = "Add exercise", onClick = viewModel::onAddExercise)
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Keypad(state: WorkoutPrototypeState, focus: CellFocus, keys: KeypadKeys) {
    val set = state.blocks.set(focus.ref) ?: return
    val timed = set.values.mode == SetMode.Time
    Column(verticalArrangement = Arrangement.spacedBy(KeypadSizes.Gap)) {
        // Which cell is being typed into, the set's Reps/Time and ⌄ to hide
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            Column(modifier = Modifier.weight(1f)) {
                AppText(text = focusLabel(state, focus), style = tagStyle(), color = Theme.contentColors.secondary, maxLines = 1)
                AppText(text = exerciseName(state, focus.ref), style = Theme.typography.bodyBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            ModeToggle(mode = set.values.mode, onModeChange = keys.onMode)
            AppIconButton(onClick = keys.onHide, icon = ImageVector.vectorResource(R.drawable.ic_keyboard_down), contentDescription = "Hide keypad")
        }
        KeyRow {
            DigitKey('1', keys)
            DigitKey('2', keys)
            DigitKey('3', keys)
            Key(text = "−", onClick = { keys.onStep(-1) })
        }
        KeyRow {
            DigitKey('4', keys)
            DigitKey('5', keys)
            DigitKey('6', keys)
            Key(text = "+", onClick = { keys.onStep(1) })
        }
        KeyRow {
            DigitKey('7', keys)
            DigitKey('8', keys)
            DigitKey('9', keys)
            Key(icon = ImageVector.vectorResource(R.drawable.ic_back), contentDescription = "Delete", onClick = keys.onBackspace)
        }
        KeyRow {
            Key(text = ".", enabled = focus.field == ValueField.Weight, onClick = { keys.onDigit('.') })
            DigitKey('0', keys)
            Key(text = "Next", onClick = keys.onNext)
            // A timed set runs its timer; anything else is completed
            if (timed && !set.done) {
                Key(text = "Start", primary = true, onClick = keys.onComplete)
            } else {
                Key(icon = ImageVector.vectorResource(R.drawable.ic_check), contentDescription = "Complete set", primary = true, onClick = keys.onComplete)
            }
        }
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(KeypadSizes.Gap), content = content)
}

@Composable
private fun RowScope.DigitKey(digit: Char, keys: KeypadKeys) {
    Key(text = "$digit", onClick = { keys.onDigit(digit) })
}

// A key in the steppers' darker inset; the primary one (complete) filled lime
@Composable
private fun RowScope.Key(
    onClick: () -> Unit,
    text: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(KeypadSizes.KeyCorner)
    val content = if (primary) Theme.contentColors.onAction else Theme.contentColors.primary
    Box(
        modifier = Modifier
            .weight(1f)
            .height(KeypadSizes.Key)
            .clip(shape)
            .background(if (primary) Theme.contentColors.action else WorkoutColors.Inset)
            .border(1.dp, if (primary) Theme.contentColors.action else Theme.backgrounds.separator, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val color = if (enabled) content else content.copy(alpha = 0.3f)
        when {
            icon != null -> AppIcon(modifier = Modifier.size(22.dp), imageVector = icon, tint = color, contentDescription = contentDescription)
            text != null && text.length == 1 -> AppText(text = text, style = numberStyle(), color = color)
            text != null -> AppText(text = text, style = Theme.typography.button, color = color)
        }
    }
}

// The countdown in progress: its label over the time, ±10s and a way to end it
@Composable
private fun TimerStrip(
    label: String,
    remainingSeconds: Int,
    color: Color,
    background: Color,
    border: Color,
    onStep: (Int) -> Unit,
    endText: String,
    onEnd: () -> Unit,
) {
    val shape = RoundedCornerShape(KeypadSizes.StripCorner)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(KeypadSizes.StripHeight)
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(text = label, style = tagStyle(), color = color)
            AppText(text = formatClock(remainingSeconds.toLong()), style = numberStyle(), color = color)
        }
        DockPill(text = "−10s", onClick = { onStep(-1) }, height = 36.dp, horizontalPadding = Theme.spacing.medium)
        DockPill(text = "+10s", onClick = { onStep(1) }, height = 36.dp, horizontalPadding = Theme.spacing.medium)
        DockPill(text = endText, onClick = onEnd, height = 36.dp, horizontalPadding = Theme.spacing.medium)
    }
}

// SET 2 OF 4 · KG, with a superset's exercise: SET 1 OF 3 · 2 OF 2 · REPS
private fun focusLabel(state: WorkoutPrototypeState, focus: CellFocus): String {
    val block = state.block(focus.ref.blockId) ?: return ""
    val round = block.rounds.indexOfFirst { it.id == focus.ref.roundId } + 1
    val exercise = if (block.exercises.size > 1) " · ${focus.ref.index + 1} OF ${block.exercises.size}" else ""
    val field = when (focus.field) {
        ValueField.Weight -> "KG"
        ValueField.Reps -> "REPS"
        ValueField.Time -> "TIME"
    }
    return "SET $round OF ${block.rounds.size}$exercise · $field"
}

private fun exerciseName(state: WorkoutPrototypeState, ref: SetRef): String =
    state.block(ref.blockId)?.exercises?.getOrNull(ref.index)?.name.orEmpty()
