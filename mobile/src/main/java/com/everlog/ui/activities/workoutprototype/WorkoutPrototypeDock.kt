package com.everlog.ui.activities.workoutprototype

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.everlog.ui.design.elements.AppMenuItem
import com.everlog.ui.design.elements.AppMenu
import com.everlog.ui.design.elements.AppIconButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.everlog.R
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.AppThumbnail
import com.everlog.ui.design.elements.LocalAppScreenHazeState
import com.everlog.ui.design.theme.Theme
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

internal class DockActions(
    val viewModel: WorkoutPrototypeViewModel,
    val onPullUp: () -> Unit,
    // The workout's options, behind the cog at the dock's bottom right
    val menuItems: List<AppMenuItem>,
)

// The dock does one job at a time
private enum class DockMode {
    Empty,
    Set,
    TimerRunning,
    Rest,
    Editing,
    AllDone,
}

private data class DockFrame(val mode: DockMode, val state: WorkoutPrototypeState)

private object DockSizes {
    val NudgeHop = 8.dp
    const val NudgeUpMillis = 120
    const val NudgeFlashMillis = 700
    val Corner = 30.dp
    val Padding = 14.dp
    // With the gap after it, sized so the name lines up with the list's text: the dock's 16dp
    // margin, its padding, this and the gap make the list's 16dp margin, 44dp spine gutter and 16dp
    // row padding
    val Thumbnail = 40.dp
    val ThumbnailGap = 6.dp
    // Concentric with the dock's corners
    val ThumbnailCorner = Corner - Padding
    val StripCorner = 22.dp
    val TrackHeight = 5.dp
    val StripHeight = 50.dp
    val StripButton = 36.dp
    val StripTrack = 3.dp
    val StripLabel = 9.sp
    val PullDistance = 40.dp
}

private fun WorkoutPrototypeState.dockMode(): DockMode = when {
    blocks.isEmpty() -> DockMode.Empty
    editing != null -> DockMode.Editing
    setTimer != null -> DockMode.TimerRunning
    rest != null -> DockMode.Rest
    current != null -> DockMode.Set
    else -> DockMode.AllDone
}

/**
 * Floating at the bottom over the blurred list, like [com.everlog.ui.design.elements.AppFooter],
 * but holding the current set's controls, or rest. Pulling it up opens Focus.
 */
@Composable
internal fun Dock(state: WorkoutPrototypeState, actions: DockActions) {
    val mode = state.dockMode()
    val shape = RoundedCornerShape(DockSizes.Corner)
    val pullDistance = with(androidx.compose.ui.platform.LocalDensity.current) { DockSizes.PullDistance.toPx() }
    // A nudge (tapping the current set's row): the dock hops up and springs back, its outline flashing
    // lime, with a haptic tick
    val hop = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    val hopHeight = with(androidx.compose.ui.platform.LocalDensity.current) { DockSizes.NudgeHop.toPx() }
    LaunchedEffect(state.dockNudges) {
        if (state.dockNudges == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        launch {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(DockSizes.NudgeFlashMillis))
        }
        hop.animateTo(-hopHeight, tween(DockSizes.NudgeUpMillis))
        hop.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    val outline = if (mode == DockMode.Editing) WorkoutColors.Editing.copy(alpha = 0.6f) else Theme.backgrounds.separator
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            // The list's 16dp side margin, so the dock lines up with its cards
            .padding(start = Theme.spacing.large, end = Theme.spacing.large, bottom = Theme.spacing.small + 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = hop.value }
                .clip(shape)
                .dockBlur()
                .border(1.dp, lerp(outline, Theme.contentColors.action, flash.value), shape)
                .pointerInput(Unit) {
                    var dragged = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = { if (dragged < -pullDistance) actions.onPullUp() },
                        onVerticalDrag = { _, amount -> dragged += amount },
                    )
                }
                .padding(DockSizes.Padding),
        ) {
            // Each mode's content keeps the state it was shown with, so the outgoing one doesn't
            // flash the next set's values while it fades out
            AnimatedContent(
                targetState = DockFrame(mode, state),
                contentKey = { it.mode },
                transitionSpec = {
                    (fadeIn(tween(150)) togetherWith fadeOut(tween(150))).using(SizeTransform(clip = false) { _, _ -> tween(250) })
                },
                label = "dock",
            ) { frame ->
                val shown = frame.state
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                    when (frame.mode) {
                        DockMode.Empty -> EmptyContent(actions)
                        DockMode.Set -> SetContent(shown, actions)
                        DockMode.TimerRunning -> TimerContent(shown, actions)
                        DockMode.Rest -> RestContent(shown, actions)
                        DockMode.Editing -> EditingContent(shown, actions)
                        DockMode.AllDone -> AllDoneContent(actions)
                    }
                }
            }
        }
    }
}

// The same blur and tint as AppFooter
@Composable
internal fun Modifier.dockBlur(): Modifier {
    val hazeState = LocalAppScreenHazeState.current
    return if (hazeState != null) {
        hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = Theme.backgrounds.primary,
                tint = HazeTint(Theme.backgrounds.blurOverlay),
                blurRadius = 20.dp,
                noiseFactor = 0f,
                fallbackTint = HazeTint(Theme.backgrounds.primary),
            ),
        )
    } else {
        background(Theme.backgrounds.primary)
    }
}

// The exercise's image and name, with its tag (SET 3 OF 4) above the name
@Composable
private fun DockHeader(
    tag: String,
    title: String,
    imageUrl: String? = null,
    // Grey like SUPERSET in the list. Editing's is white, to say the dock is changing a done set.
    tagColor: Color = Theme.contentColors.secondary,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DockSizes.ThumbnailGap)) {
        AppThumbnail(
            modifier = Modifier
                .size(DockSizes.Thumbnail)
                .clip(RoundedCornerShape(DockSizes.ThumbnailCorner)),
            imageUrl = imageUrl,
        )
        Column(modifier = Modifier.weight(1f)) {
            AppText(text = tag.uppercase(), style = tagStyle(), color = tagColor, maxLines = 1)
            AppText(text = title, style = Theme.typography.bodyBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

private fun WorkoutPrototypeState.setLabel(ref: SetRef): String {
    val block = block(ref.blockId) ?: return ""
    val round = block.rounds.indexOfFirst { it.id == ref.roundId } + 1
    val base = "Set $round of ${block.rounds.size}"
    return if (block.exercises.size > 1) "$base · ${ref.index + 1} of ${block.exercises.size}" else base
}

// A superset or circuit's says which exercise of the set it completes, like the dock's label:
// "Complete set 2 of 3 · 1 of 2"
private fun completeLabel(state: WorkoutPrototypeState, ref: SetRef): String =
    if ((state.block(ref.blockId)?.exercises?.size ?: 1) > 1) {
        "Complete ${state.setLabel(ref).replaceFirstChar { it.lowercase() }}"
    } else {
        "Complete set ${state.roundNumber(ref)}"
    }

private fun WorkoutPrototypeState.roundNumber(ref: SetRef): Int =
    (block(ref.blockId)?.rounds?.indexOfFirst { it.id == ref.roundId } ?: 0) + 1

private fun WorkoutPrototypeState.exerciseName(ref: SetRef): String =
    block(ref.blockId)?.exercises?.getOrNull(ref.index)?.name.orEmpty()

private fun WorkoutPrototypeState.exerciseImage(ref: SetRef): String? =
    block(ref.blockId)?.exercises?.getOrNull(ref.index)?.imageUrl

// Reps or time first, then weight
@Composable
private fun Steppers(values: SetValues, viewModel: WorkoutPrototypeViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
        when (values.mode) {
            SetMode.Reps -> Stepper(
                modifier = Modifier.weight(1f),
                value = "${values.reps}",
                label = "reps",
                onStep = { viewModel.onRepsStep(it * WorkoutPrototypeViewModel.RepsStep) },
                onValueClick = { viewModel.onEnterValue(ValueField.Reps) },
            )
            SetMode.Time -> Stepper(
                modifier = Modifier.weight(1f),
                value = formatClock(values.timeSeconds.toLong()),
                label = "time",
                onStep = { viewModel.onTimeStep(it * WorkoutPrototypeViewModel.TimeStepSeconds) },
                onValueClick = { viewModel.onEnterValue(ValueField.Time) },
            )
        }
        Stepper(
            modifier = Modifier.weight(1f),
            value = formatWeight(values.weightKg),
            label = "kg",
            onStep = { viewModel.onWeightStep(it * WorkoutPrototypeViewModel.WeightStepKg) },
            onValueClick = { viewModel.onEnterValue(ValueField.Weight) },
        )
    }
}

// The dock's last row: Add exercise at the left, the workout's options (cog) at the right. Both
// sit in the steppers' darker inset with a thin outline.
@Composable
private fun DockFooterRow(
    actions: DockActions,
    showAddExercise: Boolean = true,
    // In place of the cog, e.g. Restart timer while editing a timed set
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (showAddExercise) DockPill(text = "Add exercise", onClick = actions.viewModel::onAddExercise)
        Spacer(modifier = Modifier.weight(1f))
        if (trailing != null) {
            trailing()
            return@Row
        }
        var menuOpen by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .outlinedCircle()
                .clip(CircleShape)
                .background(WorkoutColors.Inset),
        ) {
            AppIconButton(
                onClick = { menuOpen = true },
                icon = ImageVector.vectorResource(R.drawable.ic_settings),
                contentDescription = "Workout options",
            )
            AppMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, items = actions.menuItems)
        }
    }
}

@Composable
private fun EmptyContent(actions: DockActions) {
    DockHeader(tag = "No exercises yet", title = "Start with one exercise")
    AppButton(modifier = Modifier.fillMaxWidth(), onClick = actions.viewModel::onAddExercise, text = "Add exercise")
    DockFooterRow(actions, showAddExercise = false)
}

@Composable
private fun SetContent(state: WorkoutPrototypeState, actions: DockActions) {
    val viewModel = actions.viewModel
    val ref = state.current ?: return
    val values = state.blocks.set(ref)?.values ?: return
    DockHeader(
        tag = state.setLabel(ref),
        title = state.exerciseName(ref),
        imageUrl = state.exerciseImage(ref),
        trailing = { ModeToggle(mode = values.mode, onModeChange = viewModel::onModeChanged) },
    )
    Steppers(values, viewModel)
    when (values.mode) {
        SetMode.Reps -> AppButton(modifier = Modifier.fillMaxWidth(), onClick = viewModel::onCompleteSet, text = completeLabel(state, ref))
        // A timed set can only be completed by running its timer
        SetMode.Time -> AppButton(modifier = Modifier.fillMaxWidth(), onClick = viewModel::onStartTimer, text = "Start timer")
    }
    DockFooterRow(actions)
}

@Composable
private fun TimerContent(state: WorkoutPrototypeState, actions: DockActions) {
    val viewModel = actions.viewModel
    val timer = state.setTimer ?: return
    val total = timer.totalSeconds
    val remaining = ceilSeconds(timer.remainingMillis(state.now))
    val action = Theme.contentColors.action
    DockHeader(tag = state.setLabel(timer.ref), title = state.exerciseName(timer.ref), imageUrl = state.exerciseImage(timer.ref))
    TimerStrip(
        label = "SET TIMER",
        labelColor = action,
        remainingSeconds = remaining,
        fraction = remaining / total.coerceAtLeast(1).toFloat(),
        color = action,
        background = Theme.backgrounds.selected,
        border = Theme.backgrounds.actionMuted,
    ) {
        TimerStepPills(onStep = { viewModel.onTimerStep(it * WorkoutPrototypeViewModel.TimerStepSeconds) })
    }
    AppSecondaryButton(modifier = Modifier.fillMaxWidth(), onClick = viewModel::onFinishTimerEarly, text = "Finish early")
    DockFooterRow(actions)
}

// Laid out like the set timer: the next set where the set timer has its exercise, then the same
// strip in amber
@Composable
private fun RestContent(state: WorkoutPrototypeState, actions: DockActions) {
    val viewModel = actions.viewModel
    val rest = state.rest ?: return
    val remaining = state.restRemainingSeconds()
    state.current?.let { next ->
        DockHeader(
            tag = "Up next · ${state.setLabel(next)}",
            title = state.exerciseName(next),
            imageUrl = state.exerciseImage(next),
        )
    }
    TimerStrip(
        label = "REST",
        labelColor = WorkoutColors.Rest,
        remainingSeconds = remaining,
        fraction = remaining / rest.totalSeconds.coerceAtLeast(1).toFloat(),
        color = WorkoutColors.Rest,
        background = WorkoutColors.RestMuted,
        border = WorkoutColors.RestBorder,
    ) {
        TimerStepPills(onStep = { viewModel.onRestStep(it * WorkoutPrototypeViewModel.RestStepSeconds) })
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(Theme.shapes.button)
            .background(WorkoutColors.Rest)
            .clickable(onClick = viewModel::onSkipRest),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = "Skip rest", style = Theme.typography.button, color = Theme.contentColors.onAction)
    }
    DockFooterRow(actions)
}

// −10s and +10s, the same on both timers
@Composable
private fun TimerStepPills(onStep: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
        DockPill(text = "−10s", onClick = { onStep(-1) }, height = DockSizes.StripButton, horizontalPadding = Theme.spacing.medium)
        DockPill(text = "+10s", onClick = { onStep(1) }, height = DockSizes.StripButton, horizontalPadding = Theme.spacing.medium)
    }
}

// A countdown the height of the steppers row: its label over the time, its buttons at the end and
// how much is left along the bottom edge. The set timer's is lime, rest's amber, labels included.
@Composable
private fun TimerStrip(
    label: String,
    labelColor: Color,
    remainingSeconds: Int,
    fraction: Float,
    color: Color,
    background: Color,
    border: Color,
    buttons: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(DockSizes.StripCorner)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(DockSizes.StripHeight)
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AppText(text = label, style = tagStyle().copy(fontSize = DockSizes.StripLabel), color = labelColor)
                AppText(text = formatClock(remainingSeconds.toLong()), style = numberStyle(), color = color)
            }
            buttons()
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(DockSizes.StripTrack)
                .background(color),
        )
    }
}

// Changing a done set. Done saves it (Back leaves it as it was). A timed set can also be run again:
// Restart timer makes it the current set, not done, with its timer counting down.
@Composable
private fun EditingContent(state: WorkoutPrototypeState, actions: DockActions) {
    val viewModel = actions.viewModel
    val editing = state.editing ?: return
    val number = state.roundNumber(editing.ref)
    DockHeader(
        tag = "Editing set $number",
        title = state.exerciseName(editing.ref),
        imageUrl = state.exerciseImage(editing.ref),
        trailing = { ModeToggle(mode = editing.values.mode, onModeChange = viewModel::onModeChanged) },
    )
    Steppers(editing.values, viewModel)
    AppButton(modifier = Modifier.fillMaxWidth(), onClick = viewModel::onSaveEdit, text = "Done")
    DockFooterRow(
        actions,
        trailing = if (editing.values.mode == SetMode.Time) {
            { DockPill(text = "Restart timer", onClick = viewModel::onRestartTimer) }
        } else {
            null
        },
    )
}

@Composable
private fun AllDoneContent(actions: DockActions) {
    val viewModel = actions.viewModel
    DockHeader(tag = "All sets done", title = "Finish when you're ready")
    AppButton(modifier = Modifier.fillMaxWidth(), onClick = viewModel::onFinish, text = "Finish")
    DockFooterRow(actions)
}
