package com.everlog.ui.activities.workoutlog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.AppThumbnail
import com.everlog.ui.design.theme.Theme
import kotlinx.coroutines.delay

// The table's sizes, local to the workout screen
private object TableSizes {
    val Thumbnail = 40.dp
    val ThumbnailCorner = 12.dp
    // A group's images: one per name, its letter (as in 1a, 1b) on the corner
    val GroupThumbnail = 24.dp
    val GroupThumbnailCorner = 8.dp
    val GroupRowGap = 6.dp
    val Badge = 16.dp
    val BadgeOffset = 3.dp
    val RowHeight = 52.dp
    val RowPadding = 12.dp
    val SetColumn = 32.dp
    val Cell = 72.dp
    val CellHeight = 36.dp
    val Check = 36.dp
    val CheckDone = 32.dp
    // The tick's tap area: wider than it looks, and the row's full height
    val CheckTarget = 44.dp
    val ColumnGap = 8.dp
    val FocusOutline = 1.5.dp
    val CheckIcon = 18.dp
    const val KeypadOpenMillis = 350L
}

// The keypad's inset, for the selected row's boxes and the ticks, so they read as what the keypad types into
private val Inset = Color.Black.copy(alpha = 0.32f)

/**
 * The workout as a table: per exercise its header, then a card of rows, SET · REPS (or TIME) · KG · tick. A superset
 * or circuit has a row per exercise in each round, in its header's order. Tapping a value focuses it for the keypad;
 * the tick completes the row, or undoes it.
 */
@Composable
internal fun WorkoutTable(
    blocks: List<WorkoutBlock>,
    unit: WeightUnit,
    focus: CellFocus?,
    selected: SetRef?,
    typed: String?,
    // Goes up with every keypad key, to bring the edited cell back into view
    reveal: Int,
    contentPadding: PaddingValues,
    onCellTapped: (CellFocus) -> Unit,
    onTick: (SetRef) -> Unit,
) {
    // The edited cell is kept in view: below the table's top, and above the keypad, which floats over the table
    val listState = rememberLazyListState()
    var listTop by remember { mutableStateOf(0f) }
    var listBottom by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val keepClear = with(density) { (contentPadding.calculateBottomPadding() + Theme.spacing.extraLarge).toPx() }
    val margin = with(density) { Theme.spacing.large.toPx() }
    val ensureVisible: suspend (Float, Float) -> Unit = { cellTop, cellBottom ->
        val below = cellBottom - (listBottom - keepClear)
        val above = (listTop + margin) - cellTop
        when {
            below > 0 -> listState.animateScrollBy(below)
            above > 0 -> listState.animateScrollBy(-above)
        }
    }
    // An exercise scrolled far enough away isn't laid out at all, so first scroll to it
    LaunchedEffect(focus?.ref?.blockId, reveal) {
        val blockId = focus?.ref?.blockId ?: return@LaunchedEffect
        val index = blocks.indexOfFirst { it.id == blockId }
        if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            listState.animateScrollToItem(index)
        }
    }
    CompositionLocalProvider(LocalEnsureVisible provides ensureVisible, LocalReveal provides reveal) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(WorkoutLogTags.Table)
                .onGloballyPositioned {
                    val bounds = it.boundsInWindow()
                    listTop = bounds.top
                    listBottom = bounds.bottom
                },
            state = listState,
            contentPadding = PaddingValues(
                start = Theme.spacing.large,
                end = Theme.spacing.large,
                top = Theme.spacing.medium,
                bottom = contentPadding.calculateBottomPadding() + Theme.spacing.large,
            ),
        ) {
            blocks.forEachIndexed { b, block ->
                item(key = block.id) {
                    ExerciseTable(b, block, unit, focus, selected, typed, onCellTapped, onTick)
                }
            }
        }
    }
}

// Scrolls the table so a cell at these window positions (top, bottom) is in view, clear of the keypad
private val LocalEnsureVisible = staticCompositionLocalOf<suspend (Float, Float) -> Unit> { { _, _ -> } }

// The table's reveal count, for the focused cell to bring itself into view again
private val LocalReveal = staticCompositionLocalOf { 0 }

@Composable
private fun ExerciseTable(
    // The block's place in the workout, for the rows' test tags
    index: Int,
    block: WorkoutBlock,
    unit: WeightUnit,
    focus: CellFocus?,
    selected: SetRef?,
    typed: String?,
    onCellTapped: (CellFocus) -> Unit,
    onTick: (SetRef) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = Theme.spacing.extraLarge)) {
        ExerciseHeader(block)
        Spacer(modifier = Modifier.height(Theme.spacing.medium))
        ColumnLabels(block, unit)
        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Theme.shapes.surface)
                .background(Theme.backgrounds.surface),
        ) {
            block.rounds.forEachIndexed { r, round ->
                if (r > 0) HorizontalDivider(thickness = 1.dp, color = Theme.backgrounds.separator)
                val firstShown = round.sets.indexOfFirst { !it.removed }
                round.sets.forEachIndexed { i, set ->
                    if (set.removed) return@forEachIndexed
                    val ref = SetRef(block.id, round.id, i)
                    key(round.id, i) {
                        TableRow(
                            tag = WorkoutLogTags.set(index, r, i),
                            // A superset or circuit's rows are 1a, 1b, 1c: the letter is the exercise's place
                            number = if (round.sets.size > 1) "${r + 1}${'a' + i}" else if (i == firstShown) "${r + 1}" else null,
                            set = set,
                            weightText = cellText(set, ValueField.Weight, unit, focus, typed, ref),
                            countText = cellText(set, countField(set), unit, focus, typed, ref),
                            // The keypad's main key will complete the set: its tick lights up to show what's next
                            armed = !set.done && focus == CellFocus(ref, ValueField.Weight),
                            selected = selected == ref,
                            weightFocused = focus == CellFocus(ref, ValueField.Weight),
                            countFocused = focus?.ref == ref && focus.field != ValueField.Weight,
                            onRow = { onCellTapped(CellFocus(ref, countField(set))) },
                            onWeight = { onCellTapped(CellFocus(ref, ValueField.Weight)) },
                            onCount = { onCellTapped(CellFocus(ref, countField(set))) },
                            onTick = { onTick(ref) },
                        )
                    }
                }
            }
        }
    }
}

// The exercise's image and name, or a group's type over one row per exercise
@Composable
private fun ExerciseHeader(block: WorkoutBlock) {
    Row(
        modifier = Modifier.padding(horizontal = TableSizes.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        if (block.exercises.size == 1) {
            val exercise = block.exercises.first().exercise
            AppThumbnail(
                modifier = Modifier.size(TableSizes.Thumbnail).clip(RoundedCornerShape(TableSizes.ThumbnailCorner)),
                imageUrl = exercise?.imageUrl,
            )
            AppText(
                modifier = Modifier.weight(1f),
                text = exercise?.name.orEmpty(),
                style = Theme.typography.heading,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            GroupHeader(modifier = Modifier.weight(1f), block = block)
        }
    }
}

// SUPERSET, CIRCUIT and so on over the names, each with its own image and letter
@Composable
private fun GroupHeader(modifier: Modifier, block: WorkoutBlock) {
    val context = LocalContext.current
    val type = block.setType
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TableSizes.GroupRowGap)) {
        type?.getTitle(context)?.let { title ->
            AppText(
                modifier = Modifier.padding(start = TableSizes.GroupThumbnail + Theme.spacing.medium),
                text = title.uppercase(),
                style = Theme.typography.small,
                color = Theme.contentColors.secondary,
            )
        }
        block.exercises.forEachIndexed { i, exercise ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium)) {
                Box {
                    AppThumbnail(
                        modifier = Modifier.size(TableSizes.GroupThumbnail).clip(RoundedCornerShape(TableSizes.GroupThumbnailCorner)),
                        imageUrl = exercise.exercise?.imageUrl,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = TableSizes.BadgeOffset, y = TableSizes.BadgeOffset)
                            .size(TableSizes.Badge)
                            .background(Theme.backgrounds.primary, CircleShape)
                            .border(1.dp, Theme.backgrounds.separator, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppText(text = ('a' + i).toString(), style = Theme.typography.small, color = Theme.contentColors.primary)
                    }
                }
                AppText(
                    text = exercise.exercise?.name.orEmpty(),
                    style = Theme.typography.heading,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ColumnLabels(block: WorkoutBlock, unit: WeightUnit) {
    val modes = block.rounds.flatMap { round -> round.sets.filterNot { it.removed }.map { it.mode } }.toSet()
    val count = when {
        modes == setOf(SetMode.Time) -> stringResource(R.string.workout_log_column_time)
        SetMode.Time in modes -> stringResource(R.string.workout_log_column_reps_time)
        else -> stringResource(R.string.workout_log_column_reps)
    }
    Row(
        modifier = Modifier.padding(horizontal = TableSizes.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(TableSizes.ColumnGap),
    ) {
        Label(stringResource(R.string.workout_log_column_set), Modifier.width(TableSizes.SetColumn), TextAlign.Center)
        Spacer(modifier = Modifier.weight(1f))
        Label(count, Modifier.width(TableSizes.Cell), TextAlign.Center)
        Label(unitLabel(unit), Modifier.width(TableSizes.Cell), TextAlign.Center)
        Spacer(modifier = Modifier.width(TableSizes.CheckTarget))
    }
}

@Composable
private fun Label(text: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    AppText(modifier = modifier, text = text, style = Theme.typography.small, color = Theme.contentColors.secondary, textAlign = align, maxLines = 1)
}

// The selected row (the current set, or the set last touched) is tinted and shows its values as boxes to type into
@Composable
private fun TableRow(
    tag: String,
    number: String?,
    set: WorkoutSet,
    weightText: String,
    countText: String,
    armed: Boolean,
    selected: Boolean,
    weightFocused: Boolean,
    countFocused: Boolean,
    onRow: () -> Unit,
    onWeight: () -> Unit,
    onCount: () -> Unit,
    onTick: () -> Unit,
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
            textAlign = TextAlign.Center,
            text = number.orEmpty(),
            style = Theme.typography.bodyBold,
        )
        Spacer(modifier = Modifier.weight(1f))
        // Reps (or time) first, then weight: 15 × 17 kg
        Cell(tag = WorkoutLogTags.count(tag), text = countText, focused = countFocused, boxed = selected, onClick = onCount)
        Cell(tag = WorkoutLogTags.weight(tag), text = weightText, focused = weightFocused, boxed = selected, onClick = onWeight)
        Tick(tag = WorkoutLogTags.tick(tag), done = set.done, armed = armed, onClick = onTick)
    }
}

// A value, boxed to type into on the selected row (the focused one outlined and kept in view above the keypad),
// plain text on the others
@Composable
private fun Cell(tag: String, text: String, focused: Boolean, boxed: Boolean, onClick: () -> Unit) {
    val shape = Theme.shapes.button
    // The whole row's height takes the tap; the press shows on the box
    val interactions = remember { MutableInteractionSource() }
    var top by remember { mutableStateOf(0f) }
    var bottom by remember { mutableStateOf(0f) }
    val ensureVisible by rememberUpdatedState(LocalEnsureVisible.current)
    // When focused, and on every key after: once the keypad has opened, so the cell ends up above it
    LaunchedEffect(focused, LocalReveal.current) {
        if (focused) {
            delay(TableSizes.KeypadOpenMillis)
            ensureVisible(top, bottom)
        }
    }
    Box(
        modifier = Modifier
            .size(TableSizes.Cell, TableSizes.RowHeight)
            .testTag(tag)
            .clickable(interactionSource = interactions, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .onGloballyPositioned {
                    top = it.positionInWindow().y
                    bottom = top + it.size.height
                }
                .size(TableSizes.Cell, TableSizes.CellHeight)
                .clip(shape)
                .indication(interactions, ripple())
                .then(
                    if (boxed) {
                        Modifier
                            .background(Inset)
                            .border(
                                if (focused) TableSizes.FocusOutline else 1.dp,
                                if (focused) Theme.contentColors.action else Theme.backgrounds.separator,
                                shape,
                            )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            AppText(text = text, style = Theme.typography.body.copy(fontFeatureSettings = "tnum"), maxLines = 1)
        }
    }
}

@Composable
private fun Tick(tag: String, done: Boolean, armed: Boolean, onClick: () -> Unit) {
    val shape = CircleShape
    // The tap area is the row's height and a little wider than the tick; the press shows on the tick
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(TableSizes.CheckTarget, TableSizes.RowHeight)
            .testTag(tag)
            .clickable(interactionSource = interactions, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // A solid fill reads bigger than an outline of the same size, so the done one is a little smaller
        Box(
            modifier = Modifier
                .size(if (done) TableSizes.CheckDone else TableSizes.Check)
                .clip(shape)
                .indication(interactions, ripple())
                .background(if (done) Theme.contentColors.action else Inset)
                .then(
                    when {
                        done -> Modifier
                        armed -> Modifier.border(TableSizes.FocusOutline, Theme.contentColors.action, shape)
                        else -> Modifier.border(1.dp, Theme.backgrounds.separator, shape)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                modifier = Modifier.size(TableSizes.CheckIcon),
                imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                tint = when {
                    done -> Theme.contentColors.onAction
                    armed -> Theme.contentColors.action
                    else -> Theme.contentColors.secondary
                },
                contentDescription = stringResource(if (done) R.string.workout_log_undo_set else R.string.workout_log_complete_set),
            )
        }
    }
}

// The cell's value, or what's being typed into it
private fun cellText(set: WorkoutSet, field: ValueField, unit: WeightUnit, focus: CellFocus?, typed: String?, ref: SetRef): String {
    val typing = typed?.takeIf { focus == CellFocus(ref, field) }
    return when (field) {
        ValueField.Weight -> typing ?: set.weightKg?.let { formatWeight(displayWeight(it, unit)) } ?: Empty
        ValueField.Reps -> typing ?: set.reps?.toString() ?: Empty
        ValueField.Time -> (typing?.let { typedSeconds(it) } ?: set.timeSeconds)?.let { formatClock(it.toLong()) } ?: Empty
    }
}

private const val Empty = "–"

/**
 * Test tags for the table's cells and ticks, by position: the exercise group, the round and the exercise in it.
 */
internal object WorkoutLogTags {
    const val Keypad = "keypad"
    const val Table = "table"

    fun set(block: Int, round: Int, exercise: Int) = "set:$block:$round:$exercise"

    fun count(set: String) = "$set:count"

    fun weight(set: String) = "$set:weight"

    fun tick(set: String) = "$set:tick"
}
