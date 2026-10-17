package com.everlog.ui.activities.workouttable

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.ripple
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.everlog.ui.activities.workoutprototype.WorkoutSizes
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
    // A group's images: one per name, its letter (as in 1a, 1b) on the corner
    val GroupThumbnail = 24.dp
    val GroupThumbnailCorner = 8.dp
    val GroupRowGap = 6.dp
    val Badge = 14.dp
    val BadgeOffset = 3.dp
    val BadgeText = 9.sp
    val RowHeight = 52.dp
    val RowPadding = 12.dp
    val SetColumn = 32.dp
    val Cell = 72.dp
    val CellHeight = 36.dp
    val CellCorner = 10.dp
    val Check = 36.dp
    val CheckDone = 32.dp
    // The tick and timer button's tap area: wider than they look (and the row's full height)
    val CheckTarget = 44.dp
    val ColumnGap = 8.dp
    // Extra room between the set number (1a, its effort under it) and last time
    val AfterSet = 8.dp
    val RestHeight = 32.dp
    val FocusOutline = 1.5.dp
    const val KeypadOpenMillis = 350L
    const val PulseMillis = 900
    val Mark = 9.sp
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
    // Goes up with every keypad key, to bring the edited cell back into view
    reveal: Int,
    contentPadding: PaddingValues,
    onCellTapped: (CellFocus) -> Unit,
    onSelect: (SetRef) -> Unit,
    viewModel: WorkoutPrototypeViewModel,
) {
    // The edited cell is kept in view: below the table's top, and above the keypad, which floats over
    // the table (its visible bottom is its own bottom less the footer's height)
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
        val index = state.blocks.indexOfFirst { it.id == blockId }
        if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            listState.animateScrollToItem(index)
        }
    }
    CompositionLocalProvider(LocalEnsureVisible provides ensureVisible, LocalReveal provides reveal) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
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
        state.blocks.forEach { block ->
            item(key = block.id) {
                ExerciseTable(block, state, focus, selected, typed, onCellTapped, onSelect, viewModel)
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
                val firstShown = round.sets.indexOfFirst { !it.removed }
                round.sets.forEachIndexed { i, set ->
                    if (set.removed) return@forEachIndexed
                    val ref = SetRef(block.id, round.id, i)
                    // Swiping a set to the left deletes it: in a superset just that exercise's set, the
                    // whole round once they're all gone. With Undo. Keyed, so the next set doesn't take
                    // over the swiped one's state once it's gone.
                    key(round.id, i) {
                    SwipeToDelete(onDelete = { viewModel.onDeleteSet(ref) }) {
                            val timer = state.setTimer?.takeIf { it.ref == ref }
                            TableRow(
                                laterIdeas = state.laterIdeas,
                                // A superset or circuit's rows are 1a, 1b, 1c: the letter is the exercise's place
                                number = if (round.sets.size > 1) "${r + 1}${'a' + i}" else if (i == firstShown) "${r + 1}" else null,
                                set = set,
                                suggested = state.isSuggested(set),
                                weightText = cellText(set, ValueField.Weight, focus, typed, ref),
                                // A running set timer counts down in its time cell
                                countText = timer?.let { formatClock(ceilSeconds(it.remainingMillis(state.now)).toLong()) }
                                    ?: cellText(set, countFieldOf(set), focus, typed, ref),
                                timing = timer != null,
                                // The keypad's main key will complete the set: the tick, or a timed set's timer
                                // button, lights up to show what's next. That's on kg for reps; on time for a timed
                                // set (Start), and anywhere in it while its timer runs.
                                armed = !set.done && focus?.ref == ref && when {
                                    timer != null -> true
                                    set.values.mode == SetMode.Time -> focus.field == ValueField.Time
                                    else -> focus.field == ValueField.Weight
                                },
                                selected = selected == ref,
                                onRow = { onCellTapped(CellFocus(ref, countFieldOf(set))) },
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
            }
        }
        AppTertiaryButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { viewModel.onAddSet(block.id) },
            text = "Add set",
        )
    }
}

// The exercise's image and name, or a group's one per line, and the menu
@Composable
private fun ExerciseHeader(block: PrototypeBlock, viewModel: WorkoutPrototypeViewModel) {
    // The image starting where the SET label does (centred in its column), the menu over the ticks
    val measurer = rememberTextMeasurer()
    val labelStyle = tagStyle()
    val density = LocalDensity.current
    val setLabelInset = remember(labelStyle, density) {
        val width = with(density) { measurer.measure("SET", labelStyle).size.width.toDp() }
        (TableSizes.SetColumn - width) / 2
    }
    var lastRowCentre by remember { mutableStateOf<Dp?>(null) }
    Row(
        modifier = Modifier.padding(
            start = TableSizes.RowPadding + setLabelInset,
            end = TableSizes.RowPadding + (TableSizes.CheckTarget - WorkoutSizes.OutlinedButton) / 2,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        if (block.kind == BlockKind.Single) {
            AppThumbnail(
                modifier = Modifier.size(TableSizes.Thumbnail).clip(RoundedCornerShape(TableSizes.ThumbnailCorner)),
                imageUrl = block.exercises.firstOrNull()?.imageUrl,
            )
            AppText(
                modifier = Modifier.weight(1f),
                text = block.exercises.firstOrNull()?.name.orEmpty(),
                style = headerTitleStyle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            GroupHeader(modifier = Modifier.weight(1f), block = block, onLastRowCentre = { lastRowCentre = it })
        }
        AppOverflowMenu(
            // A group's menu lines up with its last exercise, just above the column labels
            modifier = if (block.kind == BlockKind.Single) {
                Modifier.outlinedCircle()
            } else {
                Modifier
                    .align(Alignment.Top)
                    .padding(top = lastRowCentre?.let { (it - WorkoutSizes.OutlinedButton / 2).coerceAtLeast(0.dp) } ?: 0.dp)
                    .outlinedCircle()
            },
            items = block.exercises.map { exercise ->
                AppMenuItem(text = "${exercise.name} details", onClick = { viewModel.onExerciseDetails(exercise) })
            } + AppMenuItem(text = "Delete", onClick = { viewModel.onDeleteBlock(block.id) }),
        )
    }
}

@Composable
private fun headerTitleStyle() = Theme.typography.heading.copy(fontSize = (Theme.typography.heading.fontSize.value - 2).sp)

// SUPERSET or CIRCUIT over the names, each with its own image and letter
@Composable
private fun GroupHeader(modifier: Modifier, block: PrototypeBlock, onLastRowCentre: (Dp) -> Unit) {
    val density = LocalDensity.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TableSizes.GroupRowGap)) {
        AppText(
            modifier = Modifier.padding(start = TableSizes.GroupThumbnail + Theme.spacing.medium),
            text = if (block.kind == BlockKind.Circuit) "CIRCUIT" else "SUPERSET",
            style = tagStyle(),
            color = Theme.contentColors.secondary,
        )
        block.exercises.forEachIndexed { i, exercise ->
            Row(
                modifier = if (i == block.exercises.lastIndex) {
                    Modifier.onPlaced { with(density) { onLastRowCentre((it.positionInParent().y + it.size.height / 2f).toDp()) } }
                } else {
                    Modifier
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
            ) {
                Box {
                    AppThumbnail(
                        modifier = Modifier
                            .size(TableSizes.GroupThumbnail)
                            .clip(RoundedCornerShape(TableSizes.GroupThumbnailCorner)),
                        imageUrl = exercise.imageUrl,
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
                        AppText(
                            text = ('a' + i).toString(),
                            style = tagStyle().copy(fontSize = TableSizes.BadgeText, lineHeight = TableSizes.BadgeText),
                            color = Theme.contentColors.primary,
                        )
                    }
                }
                AppText(text = exercise.name, style = headerTitleStyle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
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
        Label("SET", Modifier.width(TableSizes.SetColumn), TextAlign.Center)
        Spacer(modifier = Modifier.width(TableSizes.AfterSet))
        Label("LAST TIME", Modifier.weight(1f))
        Label(count, Modifier.width(TableSizes.Cell), TextAlign.Center)
        Label("KG", Modifier.width(TableSizes.Cell), TextAlign.Center)
        Spacer(modifier = Modifier.width(TableSizes.CheckTarget))
    }
}

@Composable
private fun Label(text: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    AppText(modifier = modifier, text = text, style = tagStyle(), color = Theme.contentColors.secondary, textAlign = align, maxLines = 1)
}

@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val delete by rememberUpdatedState(onDelete)
    val swipeState = rememberSwipeToDismissBoxState()
    LaunchedEffect(swipeState.currentValue) {
        if (swipeState.currentValue == SwipeToDismissBoxValue.EndToStart) delete()
    }
    SwipeToDismissBox(
        state = swipeState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Theme.contentColors.destructive.copy(alpha = 0.2f))
                    .padding(horizontal = Theme.spacing.large),
                contentAlignment = Alignment.CenterEnd,
            ) {
                AppText(
                    text = "Delete",
                    style = Theme.typography.bodyBold,
                    color = Theme.contentColors.destructive,
                )
            }
        },
    ) {
        // On the card's background, so the red behind only shows as it's swiped
        Box(modifier = Modifier.background(Theme.backgrounds.surface)) { content() }
    }
}

@Composable
private fun RestLine(remainingSeconds: Int) {
    Box(
        modifier = Modifier.fillMaxWidth().height(TableSizes.RestHeight).padding(horizontal = TableSizes.RowPadding),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = "Resting ${formatClock(remainingSeconds.toLong())}",
            style = Theme.typography.small.copy(fontWeight = FontWeight.ExtraBold),
            color = WorkoutColors.Rest,
        )
    }
}

// The selected row (the current set, or the set last touched) is tinted lime. Tapping the row selects it, typing into its kg. A done set has its tick filled.
@Composable
private fun TableRow(
    // Effort under the set number and ▲▼ on last time: later ideas, off unless switched on
    laterIdeas: Boolean,
    number: String?,
    set: PrototypeSet,
    // The weight is the app's suggestion, not yet confirmed
    suggested: Boolean,
    weightText: String,
    countText: String,
    timing: Boolean,
    armed: Boolean,
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
        // Centred under SET, with how hard the set felt under it once that's known
        Column(modifier = Modifier.width(TableSizes.SetColumn), horizontalAlignment = Alignment.CenterHorizontally) {
            AppText(
                textAlign = TextAlign.Center,
                text = number.orEmpty(),
                style = Theme.typography.bodyBold,
            )
            set.effort?.takeIf { laterIdeas }?.let {
                // Wider than the column if it needs to be, still centred under the number
                AppText(
                    modifier = Modifier.wrapContentWidth(unbounded = true),
                    text = it.label.uppercase(),
                    style = tagStyle(),
                    color = Theme.contentColors.secondary,
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = Modifier.width(TableSizes.AfterSet))
        AppText(
            modifier = Modifier.weight(1f),
            text = set.lastTime?.summary() ?: "—",
            style = Theme.typography.caption.copy(fontFeatureSettings = "tnum"),
            color = Theme.contentColors.secondary,
            maxLines = 1,
        )
        // Once done: whether each value went up or down on last time's
        val last = set.lastTime?.takeIf { laterIdeas && set.done && it.mode == set.values.mode }
        val countTrend = last?.let {
            if (set.values.mode == SetMode.Time) set.values.timeSeconds.compareTo(it.timeSeconds) else set.values.reps.compareTo(it.reps)
        } ?: 0
        val weightTrend = last?.let { set.values.weightKg.compareTo(it.weightKg) } ?: 0
        // Only the selected row shows its values as boxes to type into; the rest read as plain text
        // Reps (or time) first, as last time reads: 15 × 17 kg
        Cell(
            text = countText,
            focused = countFocused,
            boxed = selected,
            onClick = onCount,
            highlight = timing,
            trend = countTrend,
        )
        Cell(text = weightText, focused = weightFocused, boxed = selected, onClick = onWeight, suggested = suggested, trend = weightTrend)
        if (onStart != null) StartButton(armed = armed, onClick = onStart) else Tick(done = set.done, armed = armed, onClick = onTick)
    }
}

// A value, boxed to type into on the selected row (the focused one outlined in lime and kept in view
// above the keypad), plain text on the others.
@Composable
private fun Cell(
    text: String,
    focused: Boolean,
    boxed: Boolean,
    onClick: () -> Unit,
    highlight: Boolean = false,
    // The app's suggestion, not yet confirmed: lime, with a ✦
    suggested: Boolean = false,
    // Up (1) or down (-1) on last time, once the set is done: a small lime ▲ or coral ▼
    trend: Int = 0,
) {
    val shape = RoundedCornerShape(TableSizes.CellCorner)
    // The whole row's height takes the tap; the press shows on the box
    val interactions = remember { MutableInteractionSource() }
    // Its full extent, even when partly scrolled out of sight
    var top by remember { mutableStateOf(0f) }
    var bottom by remember { mutableStateOf(0f) }
    // The latest, measured with the keypad open rather than as it was when the cell was tapped
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
                        .background(WorkoutColors.Inset)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(
                text = text,
                style = Theme.typography.body.copy(fontFeatureSettings = "tnum"),
                color = if (highlight || suggested) Theme.contentColors.action else Theme.contentColors.primary,
                maxLines = 1,
            )
            val mark = when {
                suggested -> "✦" to Theme.contentColors.action
                trend > 0 -> "▲" to Theme.contentColors.action
                trend < 0 -> "▼" to Theme.contentColors.destructive
                else -> null
            }
            mark?.let { (glyph, color) ->
                AppText(
                    modifier = Modifier.padding(start = 2.dp),
                    text = glyph,
                    style = Theme.typography.small.copy(fontSize = TableSizes.Mark),
                    color = color,
                )
            }
        }
    }
    }
}

// In place of the tick on a timed set: runs its timer
@Composable
private fun StartButton(armed: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(TableSizes.CellCorner)
    // A bigger tap area than it looks, like the ticks
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(TableSizes.CheckTarget, TableSizes.RowHeight)
            .clickable(interactionSource = interactions, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
    Box(
        modifier = Modifier
            .size(TableSizes.Check)
            .clip(shape)
            .indication(interactions, ripple())
            .background(WorkoutColors.Inset)
            .border(
                if (armed) TableSizes.FocusOutline else 1.dp,
                if (armed) Theme.contentColors.action else Theme.backgrounds.separator,
                shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Grey like a tick not done yet, lime like it when the keypad's main key will start it
        AppIcon(
            modifier = Modifier.size(18.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_timer),
            tint = if (armed) Theme.contentColors.action else Theme.contentColors.secondary,
            contentDescription = "Start timer",
        )
    }
    }
}

@Composable
private fun Tick(done: Boolean, armed: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(TableSizes.CellCorner)
    // The tap area is the row's height and a little wider than the tick; the press shows on the tick
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(TableSizes.CheckTarget, TableSizes.RowHeight)
            .clickable(interactionSource = interactions, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // A solid fill reads bigger than an outline of the same size, so the done one is inset a
        // little to look the same size as the outlined one
        Box(
            modifier = Modifier
                .size(if (done) TableSizes.CheckDone else TableSizes.Check)
                .clip(shape)
                .indication(interactions, ripple())
                .background(if (done) Theme.contentColors.action else WorkoutColors.Inset)
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
                modifier = Modifier.size(18.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                tint = when {
                    done -> Theme.contentColors.onAction
                    armed -> Theme.contentColors.action
                    else -> Theme.contentColors.secondary
                },
            )
        }
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
