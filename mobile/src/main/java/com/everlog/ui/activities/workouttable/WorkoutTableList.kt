package com.everlog.ui.activities.workouttable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.everlog.R
import com.everlog.ui.activities.workoutprototype.BlockKind
import com.everlog.ui.activities.workoutprototype.PrototypeBlock
import com.everlog.ui.activities.workoutprototype.PrototypeSet
import com.everlog.ui.activities.workoutprototype.SetMode
import com.everlog.ui.activities.workoutprototype.SetRef
import com.everlog.ui.activities.workoutprototype.ValueField
import com.everlog.ui.activities.workoutprototype.WorkoutColors
import com.everlog.ui.activities.workoutprototype.WorkoutPrototypeState
import com.everlog.ui.activities.workoutprototype.WorkoutPrototypeViewModel
import com.everlog.ui.activities.workoutprototype.ceilSeconds
import com.everlog.ui.activities.workoutprototype.formatClock
import com.everlog.ui.activities.workoutprototype.formatWeight
import com.everlog.ui.activities.workoutprototype.outlinedCircle
import com.everlog.ui.activities.workoutprototype.summary
import com.everlog.ui.activities.workoutprototype.tagStyle
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppMenuItem
import com.everlog.ui.design.elements.AppOverflowMenu
import com.everlog.ui.design.elements.AppTertiaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.AppThumbnail
import com.everlog.ui.design.theme.Theme
import kotlinx.coroutines.delay

private object TableSizes {
    val Thumbnail = 40.dp
    val ThumbnailCorner = 12.dp
    val RowHeight = 52.dp
    val RowPadding = 12.dp
    val SetColumn = 32.dp
    val Cell = 72.dp
    val CellHeight = 36.dp
    val CellCorner = 10.dp
    val Check = 36.dp
    val ColumnGap = 8.dp
    val RestHeight = 32.dp
    val FocusOutline = 1.5.dp
    const val KeypadOpenMillis = 350L
}

/**
 * The workout as a table: per exercise its header, then a card of rows, SET · LAST TIME · KG · REPS
 * (or TIME) · tick (a timer button for a timed set not done). A superset or circuit has a row per exercise in each set, in its title's order.
 * Tapping a kg or reps cell focuses it for the keypad; the tick completes the row, or undoes it.
 */
@Composable
internal fun WorkoutTable(
    state: WorkoutPrototypeState,
    focus: CellFocus?,
    selected: SetRef?,
    typed: String?,
    contentPadding: PaddingValues,
    onCellTapped: (CellFocus) -> Unit,
    onSelect: (SetRef) -> Unit,
    viewModel: WorkoutPrototypeViewModel,
) {
    // The keypad floats over the table, so a focused cell scrolls up to clear it: the table's visible
    // bottom is its own bottom less the footer's height
    val listState = rememberLazyListState()
    var listBottom by remember { mutableStateOf(0f) }
    val keepClear = with(LocalDensity.current) { (contentPadding.calculateBottomPadding() + Theme.spacing.extraLarge).toPx() }
    val ensureVisible: suspend (Float) -> Unit = { cellBottom ->
        val overlap = cellBottom - (listBottom - keepClear)
        if (overlap > 0) listState.animateScrollBy(overlap)
    }
    CompositionLocalProvider(LocalEnsureVisible provides ensureVisible) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { listBottom = it.boundsInWindow().bottom },
        state = listState,
        contentPadding = PaddingValues(
            start = Theme.spacing.large,
            end = Theme.spacing.large,
            top = Theme.spacing.extraSmall,
            bottom = contentPadding.calculateBottomPadding() + Theme.spacing.large,
        ),
    ) {
        state.blocks.forEach { block ->
            item(key = block.id) {
                ExerciseTable(block, state, focus, selected, typed, onCellTapped, onSelect, viewModel)
            }
        }
    }
    }
}

// Scrolls the table so a cell whose bottom is at this window position clears the keypad
private val LocalEnsureVisible = staticCompositionLocalOf<suspend (Float) -> Unit> { {} }

@Composable
private fun ExerciseTable(
    block: PrototypeBlock,
    state: WorkoutPrototypeState,
    focus: CellFocus?,
    selected: SetRef?,
    typed: String?,
    onCellTapped: (CellFocus) -> Unit,
    onSelect: (SetRef) -> Unit,
    viewModel: WorkoutPrototypeViewModel,
) {
    Column(modifier = Modifier.padding(bottom = Theme.spacing.extraLarge)) {
        ExerciseHeader(block, viewModel)
        Spacer(modifier = Modifier.height(Theme.spacing.medium))
        ColumnLabels(block)
        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Theme.shapes.surface)
                .background(Theme.backgrounds.surface),
        ) {
            block.rounds.forEachIndexed { r, round ->
                if (r > 0) HorizontalDivider(thickness = 1.dp, color = Theme.backgrounds.separator)
                if (state.rest != null && state.current?.roundId == round.id) RestLine(state.restRemainingSeconds())
                round.sets.forEachIndexed { i, set ->
                    val ref = SetRef(block.id, round.id, i)
                    val timer = state.setTimer?.takeIf { it.ref == ref }
                    TableRow(
                        number = if (i == 0) r + 1 else null,
                        set = set,
                        // Nothing's lime until the first touch
                        current = selected != null && state.current?.roundId == round.id,
                        weightText = cellText(set, ValueField.Weight, focus, typed, ref),
                        // A running set timer counts down in its time cell
                        countText = timer?.let { formatClock(ceilSeconds(it.remainingMillis(state.now)).toLong()) }
                            ?: cellText(set, countFieldOf(set), focus, typed, ref),
                        timing = timer != null,
                        selected = selected == ref,
                        onRow = { onCellTapped(CellFocus(ref, ValueField.Weight)) },
                        weightFocused = focus == CellFocus(ref, ValueField.Weight),
                        countFocused = focus?.ref == ref && focus.field != ValueField.Weight,
                        onWeight = { onCellTapped(CellFocus(ref, ValueField.Weight)) },
                        onCount = { onCellTapped(CellFocus(ref, countFieldOf(set))) },
                        onTick = {
                            onSelect(ref)
                            viewModel.onToggleSetAt(ref)
                        },
                        // A timed set not done yet starts its timer from the row; once running, the tick completes it
                        onStart = if (set.values.mode == SetMode.Time && !set.done && timer == null) {
                            {
                                onSelect(ref)
                                viewModel.onStartTimerAt(ref)
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        AppTertiaryButton(
            onClick = { viewModel.onAddSet(block.id) },
            text = "Add set",
            contentPadding = PaddingValues(horizontal = TableSizes.RowPadding),
        )
    }
}

// The first exercise's image, the name (a group's one per line) and the exercise's menu
@Composable
private fun ExerciseHeader(block: PrototypeBlock, viewModel: WorkoutPrototypeViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium)) {
        AppThumbnail(
            modifier = Modifier.size(TableSizes.Thumbnail).clip(RoundedCornerShape(TableSizes.ThumbnailCorner)),
            imageUrl = block.exercises.firstOrNull()?.imageUrl,
        )
        Column(modifier = Modifier.weight(1f)) {
            when (block.kind) {
                BlockKind.Single -> Unit
                BlockKind.Superset -> AppText(text = "SUPERSET", style = tagStyle(), color = Theme.contentColors.secondary)
                BlockKind.Circuit -> AppText(text = "CIRCUIT", style = tagStyle(), color = Theme.contentColors.secondary)
            }
            val titleStyle = Theme.typography.heading.copy(fontSize = (Theme.typography.heading.fontSize.value - 2).sp)
            block.exercises.forEach {
                AppText(text = it.name, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        AppOverflowMenu(
            modifier = Modifier.outlinedCircle(),
            items = block.exercises.map { exercise ->
                AppMenuItem(text = "${exercise.name} details", onClick = { viewModel.onExerciseDetails(exercise) })
            } + AppMenuItem(text = "Delete", onClick = { viewModel.onDeleteBlock(block.id) }),
        )
    }
}

@Composable
private fun ColumnLabels(block: PrototypeBlock) {
    val modes = block.rounds.flatMap { round -> round.sets.map { it.values.mode } }.toSet()
    val count = when {
        modes == setOf(SetMode.Time) -> "TIME"
        SetMode.Time in modes -> "REPS/TIME"
        else -> "REPS"
    }
    Row(
        modifier = Modifier.padding(horizontal = TableSizes.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(TableSizes.ColumnGap),
    ) {
        Label("SET", Modifier.width(TableSizes.SetColumn))
        Label("LAST TIME", Modifier.weight(1f))
        Label("KG", Modifier.width(TableSizes.Cell), TextAlign.Center)
        Label(count, Modifier.width(TableSizes.Cell), TextAlign.Center)
        Spacer(modifier = Modifier.width(TableSizes.Check))
    }
}

@Composable
private fun Label(text: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    AppText(modifier = modifier, text = text, style = tagStyle(), color = Theme.contentColors.secondary, textAlign = align, maxLines = 1)
}

@Composable
private fun RestLine(remainingSeconds: Int) {
    Box(
        modifier = Modifier.fillMaxWidth().height(TableSizes.RestHeight).padding(horizontal = TableSizes.RowPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        AppText(
            text = "Resting ${formatClock(remainingSeconds.toLong())}",
            style = Theme.typography.small.copy(fontWeight = FontWeight.ExtraBold),
            color = WorkoutColors.Rest,
        )
    }
}

// The selected row (the set last touched) is tinted lime, and the current set's number is lime. Tapping the row selects it, typing into its kg. A done set has its tick filled.
@Composable
private fun TableRow(
    number: Int?,
    set: PrototypeSet,
    current: Boolean,
    weightText: String,
    countText: String,
    timing: Boolean,
    selected: Boolean,
    onRow: () -> Unit,
    weightFocused: Boolean,
    countFocused: Boolean,
    onWeight: () -> Unit,
    onCount: () -> Unit,
    onTick: () -> Unit,
    onStart: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TableSizes.RowHeight)
            .background(if (selected) Theme.backgrounds.selected else Color.Transparent)
            .clickable(onClick = onRow)
            .padding(horizontal = TableSizes.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TableSizes.ColumnGap),
    ) {
        AppText(
            modifier = Modifier.width(TableSizes.SetColumn),
            text = number?.let { "$it" }.orEmpty(),
            style = Theme.typography.bodyBold,
            color = if (current) Theme.contentColors.action else Theme.contentColors.primary,
        )
        AppText(
            modifier = Modifier.weight(1f),
            text = set.lastTime?.summary() ?: "—",
            style = Theme.typography.caption.copy(fontFeatureSettings = "tnum"),
            color = Theme.contentColors.secondary,
            maxLines = 1,
        )
        Cell(text = weightText, focused = weightFocused, onClick = onWeight)
        Cell(text = countText, focused = countFocused, onClick = onCount, highlight = timing)
        if (onStart != null) StartButton(onClick = onStart) else Tick(done = set.done, onClick = onTick)
    }
}

// A value to type into. The focused one is outlined in lime and kept in view above the keypad.
@Composable
private fun Cell(text: String, focused: Boolean, onClick: () -> Unit, highlight: Boolean = false) {
    val shape = RoundedCornerShape(TableSizes.CellCorner)
    var bottom by remember { mutableStateOf(0f) }
    // The latest, measured with the keypad open rather than as it was when the cell was tapped
    val ensureVisible by rememberUpdatedState(LocalEnsureVisible.current)
    // Once the keypad has opened, so the cell ends up above it rather than behind it
    LaunchedEffect(focused) {
        if (focused) {
            delay(TableSizes.KeypadOpenMillis)
            ensureVisible(bottom)
        }
    }
    Box(
        modifier = Modifier
            .onGloballyPositioned { bottom = it.boundsInWindow().bottom }
            .size(TableSizes.Cell, TableSizes.CellHeight)
            .clip(shape)
            .background(WorkoutColors.Inset)
            .border(if (focused) TableSizes.FocusOutline else 1.dp, if (focused) Theme.contentColors.action else Theme.backgrounds.separator, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = text,
            style = Theme.typography.body.copy(fontFeatureSettings = "tnum"),
            color = if (highlight) Theme.contentColors.action else Theme.contentColors.primary,
            maxLines = 1,
        )
    }
}

// In place of the tick on a timed set: runs its timer
@Composable
private fun StartButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(TableSizes.CellCorner)
    Box(
        modifier = Modifier
            .size(TableSizes.Check)
            .clip(shape)
            .background(WorkoutColors.Inset)
            .border(1.dp, Theme.contentColors.action, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            modifier = Modifier.size(18.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_timer),
            tint = Theme.contentColors.action,
            contentDescription = "Start timer",
        )
    }
}

@Composable
private fun Tick(done: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(TableSizes.CellCorner)
    Box(
        modifier = Modifier
            .size(TableSizes.Check)
            .clip(shape)
            .background(if (done) Theme.contentColors.action else WorkoutColors.Inset)
            .border(1.dp, if (done) Theme.contentColors.action else Theme.backgrounds.separator, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            modifier = Modifier.size(18.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_check),
            tint = if (done) Theme.contentColors.onAction else Theme.contentColors.secondary,
        )
    }
}

private fun countFieldOf(set: PrototypeSet) = if (set.values.mode == SetMode.Time) ValueField.Time else ValueField.Reps

// The cell's value, or what's being typed into it
private fun cellText(set: PrototypeSet, field: ValueField, focus: CellFocus?, typed: String?, ref: SetRef): String {
    val typing = typed?.takeIf { focus == CellFocus(ref, field) }
    return when (field) {
        ValueField.Weight -> typing ?: formatWeight(set.values.weightKg)
        ValueField.Reps -> typing ?: "${set.values.reps}"
        ValueField.Time -> formatClock((typing?.let { typedSeconds(it) } ?: set.values.timeSeconds).toLong())
    }
}
