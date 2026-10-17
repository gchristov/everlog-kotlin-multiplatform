package com.everlog.ui.activities.workouttable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.everlog.ui.design.elements.AppSecondaryButton
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.CircleShape
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
import com.everlog.ui.activities.workoutprototype.summary
import com.everlog.ui.activities.workoutprototype.formatWeight
import com.everlog.ui.activities.workoutprototype.SetValues
import com.everlog.ui.activities.workoutprototype.Effort
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.everlog.ui.design.elements.AppButton
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
    // Finishes editing a done set
    val onDone: () -> Unit,
)

private object KeypadSizes {
    val Corner = 30.dp
    val Padding = 14.dp
    val Key = 48.dp
    val KeyCorner = 14.dp
    val Gap = 6.dp
    val StripHeight = 50.dp
    // Same corners as the keys
    val StripCorner = KeyCorner
    // Everything in the bottom row (cog, ⌄, Reps/Time, Add exercise, Finish): AppSecondaryButton's 42dp
    val Button = 42.dp
    val Chip = 36.dp
    val ToggleInset = 3.dp
}

/**
 * Docked at the bottom like the Spine and Dock prototype's dock: a floating card over the blurred
 * table. From the top: a running set timer or rest, the keypad while it's open, and one row of
 * controls. Once a cell is tapped the keypad grows out of the card and stays open (until ⌄ hides it)
 * as focus moves from cell to cell.
 */
@Composable
internal fun TableFooter(
    state: WorkoutPrototypeState,
    viewModel: WorkoutPrototypeViewModel,
    focus: CellFocus?,
    keys: KeypadKeys,
    // The set the main action completes (or starts); none once every set is done
    target: SetRef?,
    // Opens the keypad on the selected set
    onShowKeypad: () -> Unit,
) {
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
                // Takes the touches on its empty space, so they don't reach the table behind it
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(KeypadSizes.Padding),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            // The last focus, so the keypad keeps its contents while it closes
            var shown by remember { mutableStateOf(focus) }
            if (focus != null) shown = focus
            // What's being typed into, at the top while the keypad's open, with everything else under it
            AnimatedVisibility(
                visible = focus != null,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            ) {
                shown?.let { cell ->
                    // ⌄ to close it, what's being typed into, and the set's Reps/Time
                    Row(
                        modifier = Modifier.padding(bottom = Theme.spacing.extraSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                    ) {
                        HideKeypadButton(onClick = keys.onHide)
                        Column(modifier = Modifier.weight(1f)) {
                            AppText(text = focusLabel(state, cell), style = tagStyle(), color = Theme.contentColors.secondary, maxLines = 1)
                            AppText(text = exerciseName(state, cell.ref), style = Theme.typography.bodyBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        state.blocks.set(cell.ref)?.let { KeypadModeToggle(mode = it.values.mode, onModeChange = keys.onMode) }
                    }
                }
            }
            // The suggested weight being typed into, above the timer: what it is, and why
            var why by remember { mutableStateOf(false) }
            val suggestion = focus?.takeIf { it.field == ValueField.Weight }?.let { cell ->
                state.blocks.set(cell.ref)?.takeIf { state.isSuggested(it) && it.lastTime != null }?.let { cell to it }
            }
            var shownSuggestion by remember { mutableStateOf(suggestion) }
            if (suggestion != null) shownSuggestion = suggestion
            AnimatedVisibility(visible = suggestion != null) {
                shownSuggestion?.let { (cell, set) ->
                    val last = set.lastTime ?: return@let
                    // A card like the timers', dark like the dock's other controls, the suggestion in lime
                    val cardShape = RoundedCornerShape(KeypadSizes.StripCorner)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(WorkoutColors.Inset)
                            .border(1.dp, Theme.backgrounds.separator, cardShape)
                            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                    ) {
                        AppText(
                            modifier = Modifier.weight(1f),
                            text = "✦ Suggested ${formatWeight(set.values.weightKg)} kg · ${weightChange(set.values.weightKg - last.weightKg)} from last time",
                            style = Theme.typography.caption,
                            color = Theme.contentColors.action,
                        )
                        DockPill(
                            text = "Why?",
                            onClick = { why = true },
                            height = KeypadSizes.Chip,
                            horizontalPadding = Theme.spacing.medium,
                            shape = RoundedCornerShape(KeypadSizes.KeyCorner),
                        )
                    }
                    if (why) {
                        SuggestionWhySheet(exercise = exerciseName(state, cell.ref), suggestedKg = set.values.weightKg, last = last, onDismiss = { why = false })
                    }
                }
            }
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
                ) {
                    // After a set: how hard was it? One optional tap, gone when rest ends or typing starts
                    val prompt = state.effortPrompt
                    AnimatedVisibility(visible = prompt != null && state.laterIdeas) {
                        var asked by remember { mutableStateOf(prompt) }
                        if (prompt != null) asked = prompt
                        asked?.let { ref ->
                            Row(
                                modifier = Modifier.padding(start = 14.dp, end = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                            ) {
                                AppText(text = "How hard?", style = Theme.typography.caption, color = WorkoutColors.Rest)
                                EffortChips(
                                    modifier = Modifier.weight(1f),
                                    selected = state.blocks.set(ref)?.effort,
                                    onPick = { viewModel.onSetEffort(ref, it) },
                                )
                            }
                        }
                    }
                }
            }
            // The keypad grows out of the card, above its row of controls, which stays put
            AnimatedVisibility(
                visible = focus != null,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            ) {
                shown?.let { Keypad(state, it, keys, viewModel) }
            }
            // The empty workout: where to start
            val hintText = if (focus == null && state.blocks.isEmpty()) "Add an exercise to start" else null
            // Keeps its text while it hides
            var shownHint by remember { mutableStateOf(hintText) }
            if (hintText != null) shownHint = hintText
            AnimatedVisibility(visible = hintText != null) {
                // Styled as AppFooter's header note (private to it): a card on the screen's background
                AppText(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Theme.shapes.surface)
                        .background(Theme.backgrounds.primary)
                        .padding(Theme.spacing.large),
                    text = shownHint.orEmpty(),
                    style = Theme.typography.caption,
                    color = Theme.contentColors.secondary,
                )
            }
            // One row of controls at the bottom, while the keypad's closed: the keyboard (opens the keypad),
            // Add exercise, then the main action, Complete set (or Start timer).
            // Only while the keypad's closed: open, ⌄ is beside the set at the top
            AnimatedVisibility(
                visible = focus == null,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
            AnimatedContent(
                targetState = focus != null,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "dock controls",
            ) { open ->
                val cell = shown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                ) {
                    if (open && cell != null) {
                        Unit
                    } else if (state.blocks.isEmpty()) {
                        // The empty workout's main action
                        AppButton(onClick = viewModel::onAddExercise, text = "Add exercise")
                    } else {
                        ShowKeypadButton(onClick = onShowKeypad)
                        // As tall as the main action (AppButton's 42dp)
                        DockPill(text = "Add exercise", onClick = viewModel::onAddExercise, height = KeypadSizes.Button)
                        Spacer(modifier = Modifier.weight(1f))
                        // The main action: complete the set, or start a timed one's timer. None once all are done
                        // (Finish in the header is then filled).
                        val set = target?.let { state.blocks.set(it) }
                        if (target != null && set != null) {
                            val timed = set.values.mode == SetMode.Time && state.setTimer?.ref != target
                            AppButton(
                                onClick = { if (timed) viewModel.onStartTimerAt(target) else viewModel.onCompleteSetAt(target) },
                                text = if (timed) "Start timer" else "Complete set",
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

// Reps or Time, as tall as ⌄ beside it and with the keys' corners
@Composable
private fun KeypadModeToggle(mode: SetMode, onModeChange: (SetMode) -> Unit) {
    val outer = RoundedCornerShape(KeypadSizes.KeyCorner)
    val inner = RoundedCornerShape(KeypadSizes.KeyCorner - KeypadSizes.ToggleInset)
    Row(
        modifier = Modifier
            .height(KeypadSizes.Button)
            .clip(outer)
            .background(WorkoutColors.Inset)
            .border(1.dp, Theme.backgrounds.separator, outer)
            .padding(KeypadSizes.ToggleInset),
    ) {
        listOf(SetMode.Reps to "Reps", SetMode.Time to "Time").forEach { (option, text) ->
            val selected = option == mode
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(inner)
                    .background(if (selected) Theme.contentColors.action else Color.Transparent)
                    .clickable { onModeChange(option) }
                    .padding(horizontal = Theme.spacing.medium),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = text,
                    style = Theme.typography.button,
                    color = if (selected) Theme.contentColors.onAction else Theme.contentColors.secondary,
                )
            }
        }
    }
}

// Easy, Good, Hard, Max: the picked one filled lime
@Composable
private fun EffortChips(selected: Effort?, onPick: (Effort) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
        Effort.entries.forEach { effort ->
            val on = effort == selected
            val shape = RoundedCornerShape(KeypadSizes.KeyCorner)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(KeypadSizes.Chip)
                    .clip(shape)
                    .background(if (on) Theme.contentColors.action else WorkoutColors.Inset)
                    .border(1.dp, if (on) Theme.contentColors.action else Theme.backgrounds.separator, shape)
                    .clickable { onPick(effort) },
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = effort.label,
                    style = Theme.typography.button,
                    color = if (on) Theme.contentColors.onAction else Theme.contentColors.primary,
                )
            }
        }
    }
}

// "+1 rep · +1 kg vs last time", or that it matched
private fun comparison(values: SetValues, last: SetValues): String {
    val parts = mutableListOf<String>()
    if (values.mode == last.mode) {
        if (values.mode == SetMode.Time) {
            val change = values.timeSeconds - last.timeSeconds
            if (change != 0) parts += "${if (change > 0) "+" else "−"}${kotlin.math.abs(change)}s"
        } else {
            val change = values.reps - last.reps
            if (change != 0) parts += "${if (change > 0) "+" else "−"}${kotlin.math.abs(change)} ${if (kotlin.math.abs(change) == 1) "rep" else "reps"}"
        }
    }
    val kg = values.weightKg - last.weightKg
    if (kotlin.math.abs(kg) >= 0.01f) parts += weightChange(kg)
    return if (parts.isEmpty()) "Same as last time" else parts.joinToString(" · ") + " vs last time"
}

private fun weightChange(kg: Float): String = when {
    kotlin.math.abs(kg) < 0.01f -> "same"
    kg > 0 -> "+${formatWeight(kg)} kg"
    else -> "−${formatWeight(-kg)} kg"
}

// Why this weight: last time's set and how the suggestion is made (mock)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestionWhySheet(exercise: String, suggestedKg: Float, last: SetValues, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Theme.backgrounds.primary) {
        Column(
            modifier = Modifier.padding(horizontal = Theme.spacing.large).padding(bottom = Theme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        ) {
            AppText(text = "Why ${formatWeight(suggestedKg)} kg?", style = Theme.typography.heading)
            AppText(
                text = "Last time you did ${last.summary()} on $exercise. " +
                    "The suggestion builds on that and on how your sets felt: Easy sets move you up sooner, Max holds the weight.",
                color = Theme.contentColors.secondary,
            )
            AppText(
                text = "Type any weight to use your own. You can turn suggestions off in the workout options.",
                style = Theme.typography.caption,
                color = Theme.contentColors.secondary,
            )
            AppButton(modifier = Modifier.fillMaxWidth(), onClick = onDismiss, text = "Got it")
        }
    }
}

// The cog and ⌄: a thin circle as tall as the bottom row's buttons
@Composable
private fun Modifier.controlCircle(): Modifier = this
    .size(KeypadSizes.Button)
    .border(1.dp, Theme.backgrounds.separator, CircleShape)

// ⌄, in the cog's circle and place
// The keyboard: opens the keypad on the selected set's first value. The same circle as ⌄, which
// closes it again from the same corner.
@Composable
private fun ShowKeypadButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .controlCircle()
            .clip(CircleShape)
            .background(WorkoutColors.Inset),
    ) {
        AppIconButton(onClick = onClick, icon = ImageVector.vectorResource(R.drawable.ic_keyboard), contentDescription = "Show keypad")
    }
}

@Composable
private fun HideKeypadButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .controlCircle()
            .clip(CircleShape)
            .background(WorkoutColors.Inset),
    ) {
        AppIconButton(onClick = onClick, icon = ImageVector.vectorResource(R.drawable.ic_keyboard_down), contentDescription = "Hide keypad")
    }
}

// The cog: the workout's options, in the header next to Finish
@Composable
internal fun WorkoutOptionsButton(state: WorkoutPrototypeState, viewModel: WorkoutPrototypeViewModel) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .controlCircle()
            .clip(CircleShape)
            .background(WorkoutColors.Inset),
    ) {
        AppIconButton(
            onClick = { menuOpen = true },
            icon = ImageVector.vectorResource(R.drawable.ic_settings),
            contentDescription = "Workout options",
        )
    }
    if (menuOpen) WorkoutOptionsSheet(state, viewModel, onDismiss = { menuOpen = false })
}

@Composable
private fun Keypad(state: WorkoutPrototypeState, focus: CellFocus, keys: KeypadKeys, viewModel: WorkoutPrototypeViewModel) {
    val set = state.blocks.set(focus.ref) ?: return
    val timed = set.values.mode == SetMode.Time
    // What's being typed into is at the top of the dock; Reps/Time and ⌄ in the row of controls under it
    Column(verticalArrangement = Arrangement.spacedBy(KeypadSizes.Gap)) {
        // For a done set: how it compares with last time and how hard it was
        val last = set.lastTime
        // Later ideas: off unless the prototype's switch is on
        if (set.done && state.laterIdeas) {
            last?.let { AppText(text = comparison(set.values, it), style = Theme.typography.caption, color = Theme.contentColors.secondary) }
            EffortChips(selected = set.effort, onPick = { viewModel.onSetEffort(focus.ref, it) })
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
            Key(icon = ImageVector.vectorResource(R.drawable.ic_backspace), contentDescription = "Delete", onClick = keys.onBackspace)
        }
        KeyRow {
            Key(text = ".", enabled = focus.field == ValueField.Weight, onClick = { keys.onDigit('.') })
            DigitKey('0', keys)
            // Keeps the row's gaps the same as the rows above, so "." and "0" line up with their columns
            Spacer(modifier = Modifier.width(0.dp))
            // One key that walks through the set: Next from reps to kg, then Complete set on kg, which
            // moves on to the next set
            // A timed set ends on its time instead: Next from kg to time, then Start timer, and
            // Complete set while it runs
            val running = state.setTimer?.ref == focus.ref
            when {
                timed && !running && focus.field == ValueField.Weight -> Key(text = "Next", primary = true, weight = 2f, onClick = keys.onNext)
                timed && !running -> Key(text = "Start timer", primary = true, weight = 2f, onClick = keys.onComplete)
                !timed && focus.field != ValueField.Weight -> Key(text = "Next", primary = true, weight = 2f, onClick = keys.onNext)
                // A done set's values save as they're typed, so its key just finishes editing
                set.done -> Key(text = "Done", primary = true, weight = 2f, onClick = keys.onDone)
                else -> Key(text = "Complete set", primary = true, weight = 2f, onClick = keys.onComplete)
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
    weight: Float = 1f,
) {
    val shape = RoundedCornerShape(KeypadSizes.KeyCorner)
    val content = if (primary) Theme.contentColors.onAction else Theme.contentColors.primary
    val gap = with(LocalDensity.current) { KeypadSizes.Gap.roundToPx() }
    Box(
        modifier = Modifier
            .weight(weight)
            // A key two columns wide also covers the gap between them (after the zero-width spacer)
            .then(
                if (weight > 1f) {
                    Modifier.layout { measurable, constraints ->
                        val width = constraints.maxWidth + gap
                        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                        layout(constraints.maxWidth, placeable.height) { placeable.place(-gap, 0) }
                    }
                } else {
                    Modifier
                },
            )
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
    // Under the countdown, inside the module, e.g. rest's effort question
    extra: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(KeypadSizes.StripCorner)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(KeypadSizes.StripHeight)
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AppText(text = label, style = tagStyle(), color = color)
                AppText(text = formatClock(remainingSeconds.toLong()), style = numberStyle(), color = color)
            }
            // The keys' corners, like the effort chips under them
            val keyShape = RoundedCornerShape(KeypadSizes.KeyCorner)
            DockPill(text = "−10s", onClick = { onStep(-1) }, height = KeypadSizes.Chip, horizontalPadding = Theme.spacing.medium, shape = keyShape)
            DockPill(text = "+10s", onClick = { onStep(1) }, height = KeypadSizes.Chip, horizontalPadding = Theme.spacing.medium, shape = keyShape)
            DockPill(text = endText, onClick = onEnd, height = KeypadSizes.Chip, horizontalPadding = Theme.spacing.medium, shape = keyShape)
        }
        extra?.invoke()
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
