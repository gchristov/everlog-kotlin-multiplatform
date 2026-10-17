package com.everlog.ui.activities.workoutprototype

import androidx.compose.foundation.background
import com.everlog.R
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppThumbnail
import com.everlog.ui.design.elements.AppTertiaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.list.AppListRow
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme

// What the spine's rows do
internal class SpineActions(
    val onSetTapped: (SetRef) -> Unit,
    val onAddSet: (blockId: Long) -> Unit,
    val onDeleteRound: (blockId: Long, roundId: Long) -> Unit,
    val onHistory: () -> Unit,
    val onDeleteBlock: (blockId: Long) -> Unit,
    // A set's tick: completes the whole set (every exercise of a superset's round), or undoes it
    val onToggleRound: (blockId: Long, roundId: Long) -> Unit,
    val onExerciseDetails: (PrototypeExercise) -> Unit,
) {
    companion object {
        val None = SpineActions({}, {}, { _, _ -> }, {}, {}, { _, _ -> }, {})
    }
}

private object SpineSizes {
    // Room on the left for the nodes, with the line through their centres
    val Gutter = 44.dp
    val LineX = WorkoutSizes.Node / 2
    val MarkerHeight = 30.dp
    val HeaderHeight = 48.dp
    // AppListItem's one line height, where a set's node is centred
    val CapsuleHeight = 56.dp
    // AppTertiaryButton's height
    val AddSetHeight = 42.dp
}

private sealed interface SpineItem {
    val key: Any

    // Where the node's centre is, from the row's top, for the line to start or end at
    val centre: Dp

    data class Header(val block: PrototypeBlock) : SpineItem {
        override val key = "header-${block.id}"
        override val centre = SpineSizes.HeaderHeight / 2
    }

    data class Round(val block: PrototypeBlock, val round: PrototypeRound, val number: Int) : SpineItem {
        override val key = round.id
        override val centre = SpineSizes.CapsuleHeight / 2
    }

    data class AddSet(val block: PrototypeBlock) : SpineItem {
        override val key = "add-${block.id}"
        override val centre = SpineSizes.AddSetHeight / 2
    }
}

// How the line runs through a row: lime through what's done up to the current set, a hairline after it
private enum class SpineFill {
    Done,
    UpToCentre,
    FromCentre,
    Upcoming,
}

// Lime joins sets done in a row, up to the current set: the line between two sets is lime only when
// the later one is done or current and the one before it is done. So a set skipped (or un-ticked)
// leaves the line grey on both sides, it's grey after the current set, and done sets further down
// (e.g. after going back to an earlier set) stay joined in lime. An
// exercise's Add set is lime once the line reaches its last set and that set is done.
private fun spineFills(items: List<SpineItem>, currentIndex: Int): List<SpineFill> {
    val rounds = items.indices.filter { items[it] is SpineItem.Round }
    fun done(index: Int) = (items[index] as SpineItem.Round).round.done
    // Whether the line into the set at [index] is lime
    fun litInto(index: Int?): Boolean {
        if (index == null) return false
        val previous = rounds.lastOrNull { it < index }
        return (done(index) || index == currentIndex) && (previous == null || done(previous))
    }
    fun nextRound(index: Int) = rounds.firstOrNull { it > index }
    // Whether the line from the last set before [index] to an Add set at [index] is lime
    fun litIntoAddSet(index: Int): Boolean {
        val last = rounds.lastOrNull { it < index } ?: return false
        return last != currentIndex && done(last) && litInto(last)
    }
    return items.mapIndexed { index, item ->
        when {
            item is SpineItem.Round -> {
                val top = litInto(index)
                // After the current set the line is grey
                val bottom = index != currentIndex && when (items.getOrNull(index + 1)) {
                    is SpineItem.AddSet -> litIntoAddSet(index + 1)
                    else -> litInto(nextRound(index))
                }
                when {
                    top && bottom -> SpineFill.Done
                    top -> SpineFill.UpToCentre
                    bottom -> SpineFill.FromCentre
                    else -> SpineFill.Upcoming
                }
            }
            item is SpineItem.AddSet -> {
                val top = litIntoAddSet(index)
                val bottom = litInto(nextRound(index))
                when {
                    top && bottom -> SpineFill.Done
                    top -> SpineFill.UpToCentre
                    else -> SpineFill.Upcoming
                }
            }
            // Headers sit on the line between an Add set and the next set
            else -> if (litInto(nextRound(index))) SpineFill.Done else SpineFill.Upcoming
        }
    }
}

private fun spineItems(state: WorkoutPrototypeState): List<SpineItem> = buildList {
    state.blocks.forEach { block ->
        add(SpineItem.Header(block))
        block.rounds.forEachIndexed { i, round ->
            add(SpineItem.Round(block, round, number = i + 1))
        }
        add(SpineItem.AddSet(block))
    }
}

/**
 * The workout as a timeline: a header per exercise or group, a capsule per set, Add set after the
 * last. [ghost] draws a faded example that can't be touched, for the empty workout.
 */
@Composable
internal fun SpineList(
    state: WorkoutPrototypeState,
    actions: SpineActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    ghost: Boolean = false,
) {
    val items = remember(state.blocks, state.startedAt) { spineItems(state) }
    val currentIndex = items.indexOfFirst { it is SpineItem.Round && it.round.id == state.current?.roundId }
    val fills = remember(items, state.current) { spineFills(items, currentIndex) }
    val fillOf = { index: Int -> fills[index] }

    val listState = rememberLazyListState()
    // Keeps the current set in view (above the dock) as it moves on
    LaunchedEffect(state.current) {
        if (currentIndex < 0 || ghost) return@LaunchedEffect
        val info = listState.layoutInfo
        val visible = info.visibleItemsInfo.firstOrNull { it.index == currentIndex }
        val bottom = info.viewportEndOffset - info.afterContentPadding
        if (visible == null || visible.offset < 0 || visible.offset + visible.size > bottom) {
            listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .then(if (ghost) Modifier.clearAndSetSemantics {} else Modifier),
        state = listState,
        contentPadding = PaddingValues(
            start = Theme.spacing.large,
            end = Theme.spacing.large,
            top = Theme.spacing.extraSmall,
            bottom = contentPadding.calculateBottomPadding() + Theme.spacing.large,
        ),
        userScrollEnabled = !ghost,
    ) {
        items(items.size, key = { items[it].key }) { index ->
            val item = items[index]
            // Rest's marker sits above the current set, in its row, and grows in rather than
            // arriving as a row of its own, so the sets below slide down with it in one movement
            var restHeight by remember { mutableStateOf(0.dp) }
            SpineRow(
                // No placement animation: rows below a growing rest marker follow it frame by frame,
                // where a spring lagged behind and slid them over the current set
                modifier = Modifier.animateItem(placementSpec = null),
                centre = { if (item is SpineItem.Round) restHeight + item.centre else item.centre },
                fill = fillOf(index),
                first = index == 0,
                // The line ends at the last exercise's Add set
                last = index == items.lastIndex,
            ) {
                when (item) {
                    is SpineItem.Header -> HeaderRow(item.block, fill = fillOf(index), actions = actions)
                    is SpineItem.Round -> RoundRow(item, state, actions, swipeEnabled = !ghost, onRestHeight = { restHeight = it })
                    is SpineItem.AddSet -> AddSetRow(lit = fillOf(index) != SpineFill.Upcoming, onClick = { actions.onAddSet(item.block.id) })
                }
            }
        }
    }
}

// The empty workout's faded example, faded as one so the line doesn't show through the capsules
@Composable
internal fun GhostSpine(contentPadding: PaddingValues) {
    val example = remember {
        val values = SetValues(SetMode.Reps, reps = 12, timeSeconds = 30, weightKg = 20f)
        val block = PrototypeBlock(
            id = -1,
            exercises = listOf(PrototypeExercise(-2, "Example exercise")),
            rounds = listOf(
                PrototypeRound(-3, listOf(PrototypeSet(values, done = true, lastTime = values.copy(reps = 11)))),
                PrototypeRound(-4, listOf(PrototypeSet(values, lastTime = values))),
                PrototypeRound(-5, listOf(PrototypeSet(values, lastTime = values))),
            ),
        )
        val now = System.currentTimeMillis()
        WorkoutPrototypeState(name = "", startedAt = now, now = now, blocks = listOf(block), current = SetRef(-1, -4, 0))
    }
    Column(modifier = Modifier.fillMaxSize()) {
        AppText(
            modifier = Modifier
                .padding(horizontal = Theme.spacing.large, vertical = Theme.spacing.small)
                .clip(Theme.shapes.button)
                .border(1.dp, Theme.backgrounds.separator, Theme.shapes.button)
                .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.extraSmall),
            text = "EXAMPLE · NOT YOUR DATA",
            style = tagStyle(),
            color = Theme.contentColors.secondary,
        )
        SpineList(
            modifier = Modifier.graphicsLayer {
                alpha = 0.35f
                compositingStrategy = CompositingStrategy.Offscreen
            },
            state = example,
            actions = SpineActions.None,
            contentPadding = contentPadding,
            ghost = true,
        )
    }
}

// A row with its part of the line drawn behind it, and its node in the gutter
@Composable
private fun SpineRow(
    centre: () -> Dp,
    fill: SpineFill,
    first: Boolean,
    last: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val lime = Theme.contentColors.action
    val hairline = Theme.backgrounds.separator
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val x = SpineSizes.LineX.toPx()
                val c = centre().toPx()
                val top = if (first) c else 0f
                val bottom = if (last) c else size.height
                fun line(from: Float, to: Float, done: Boolean) {
                    if (to <= from) return
                    drawLine(
                        color = if (done) lime else hairline,
                        start = Offset(x, from),
                        end = Offset(x, to),
                        // The same for what's done (lime) and what's to come (grey)
                        strokeWidth = WorkoutSizes.Line.toPx(),
                        cap = StrokeCap.Butt,
                    )
                }
                when (fill) {
                    SpineFill.Done -> line(top, bottom, done = true)
                    SpineFill.Upcoming -> line(top, bottom, done = false)
                    SpineFill.UpToCentre -> {
                        line(top, c, done = true)
                        line(c, bottom, done = false)
                    }
                    SpineFill.FromCentre -> {
                        line(top, c, done = false)
                        line(c, bottom, done = true)
                    }
                }
            },
    ) {
        content()
    }
}

// The gutter's node, centred on the line, then the row's content
@Composable
private fun GutterRow(
    node: @Composable () -> Unit,
    nodeSize: Dp,
    centre: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        // Offset rather than padded, so a node's tap target can be wider than the gutter's margin
        Box(modifier = Modifier.width(SpineSizes.Gutter)) {
            // Unbounded, so a node wider than the gutter (a set's 48dp tap target) keeps its size and
            // stays centred on the line instead of being squeezed to the gutter's width
            Box(
                modifier = Modifier
                    .offset(x = SpineSizes.LineX - nodeSize / 2, y = centre - nodeSize / 2)
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true),
            ) {
                node()
            }
        }
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

// An exercise's ⋮: a row per exercise (one, or each of a superset's) with its image, opening its
// details and history, then Delete. Looks like AppMenu, which only takes icons, not images.
@Composable
private fun ExerciseMenu(block: PrototypeBlock, actions: SpineActions) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.outlinedCircle()) {
        AppIconButton(
            onClick = { expanded = true },
            icon = ImageVector.vectorResource(R.drawable.ic_more),
            contentDescription = "Exercise options",
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = ExerciseMenuSizes.MinWidth, max = ExerciseMenuSizes.MaxWidth),
            offset = DpOffset(x = -ExerciseMenuSizes.EndInset, y = ExerciseMenuSizes.Gap),
            shape = Theme.shapes.surface,
            containerColor = Theme.backgrounds.surface,
            tonalElevation = 0.dp,
            shadowElevation = ExerciseMenuSizes.Elevation,
            border = BorderStroke(1.dp, Theme.backgrounds.separator),
        ) {
            block.exercises.forEach { exercise ->
                AppListItem(
                    title = exercise.name,
                    subtitle = "Details and history",
                    leading = {
                        AppThumbnail(
                            modifier = Modifier
                                .size(ExerciseMenuSizes.Image)
                                .clip(RoundedCornerShape(ExerciseMenuSizes.ImageCorner)),
                            imageUrl = exercise.imageUrl,
                        )
                    },
                    onClick = {
                        expanded = false
                        actions.onExerciseDetails(exercise)
                    },
                )
            }
            HorizontalDivider(thickness = 1.dp, color = Theme.backgrounds.separator)
            AppListItem(
                title = "Delete",
                titleColor = Theme.contentColors.destructive,
                onClick = {
                    expanded = false
                    actions.onDeleteBlock(block.id)
                },
            )
        }
    }
}

private object ExerciseMenuSizes {
    // As AppMenu, wider for the names
    val MinWidth = 240.dp
    val MaxWidth = 320.dp
    val EndInset = 12.dp
    val Gap = 4.dp
    val Elevation = 8.dp
    val Image = 40.dp
    val ImageCorner = 12.dp
}

@Composable
private fun HeaderRow(block: PrototypeBlock, fill: SpineFill, actions: SpineActions) {
    val hasLastTime = block.rounds.any { round -> round.sets.any { it.lastTime != null } }
    GutterRow(
        node = { SpineDot(if (fill == SpineFill.Upcoming) Theme.backgrounds.separator else Theme.contentColors.action) },
        nodeSize = WorkoutSizes.Dot,
        centre = SpineSizes.HeaderHeight / 2,
    ) {
        Column {
            // Grows for a group's name on two lines. Inset like the rows' text, so the name lines up with
            // the sets' values and the menu with their last time.
            Row(
                modifier = Modifier
                    .heightIn(min = SpineSizes.HeaderHeight)
                    .padding(horizontal = Theme.spacing.large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    when (block.kind) {
                        BlockKind.Single -> Unit
                        BlockKind.Superset -> AppText(text = "SUPERSET", style = tagStyle(), color = Theme.contentColors.secondary)
                        BlockKind.Circuit -> AppText(text = "CIRCUIT", style = tagStyle(), color = Theme.contentColors.secondary)
                    }
                    // heading 2sp smaller. A group's exercises go one per line, in the order of its rows.
                    val titleStyle = Theme.typography.heading.copy(fontSize = (Theme.typography.heading.fontSize.value - 2).sp)
                    if (block.exercises.size == 1) {
                        AppText(text = block.exercises[0].name, style = titleStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    } else {
                        block.exercises.forEach {
                            AppText(text = it.name, style = titleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                ExerciseMenu(block = block, actions = actions)
            }
            // Over the rows' two columns, lined up with their text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Theme.spacing.large,
                        end = Theme.spacing.large,
                        top = Theme.spacing.small,
                        bottom = Theme.spacing.medium,
                    ),
            ) {
                AppText(modifier = Modifier.weight(1f), text = "THIS TIME", style = tagStyle(), color = Theme.contentColors.secondary)
                if (hasLastTime) {
                    // Opens the history, but grey like THIS TIME: lime is kept for progress and the
                    // main actions
                    AppText(
                        modifier = Modifier.clickable(onClick = actions.onHistory),
                        text = "LAST TIME",
                        style = tagStyle(),
                        color = Theme.contentColors.secondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// Rest's marker, in the exercise's card above the current set. The first row of the card gets its
// top corners.
@Composable
private fun RestRow(remainingSeconds: Int, first: Boolean) {
    GutterRow(
        node = { RestNode() },
        nodeSize = WorkoutSizes.SmallNode,
        centre = SpineSizes.MarkerHeight / 2,
    ) {
        // Lined up with the sets' values
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(if (first) Theme.shapes.groupStart else Theme.shapes.groupMiddle)
                .background(Theme.backgrounds.surface)
                .height(SpineSizes.MarkerHeight)
                .padding(horizontal = Theme.spacing.large),
            contentAlignment = Alignment.CenterStart,
        ) {
            AppText(
                text = "Resting ${formatClock(remainingSeconds.toLong())}",
                style = Theme.typography.small.copy(fontWeight = FontWeight.ExtraBold),
                color = WorkoutColors.Rest,
            )
        }
    }
}

// A node on the line, lime once the line reaches it, then the button with its text lined up with
// the sets' values
@Composable
private fun AddSetRow(lit: Boolean, onClick: () -> Unit) {
    GutterRow(
        modifier = Modifier.padding(bottom = Theme.spacing.extraLarge),
        node = { Box(modifier = Modifier.clip(CircleShape).clickable(onClick = onClick)) { AddNode(lit) } },
        nodeSize = WorkoutSizes.Node,
        centre = SpineSizes.AddSetHeight / 2,
    ) {
        Box(modifier = Modifier.height(SpineSizes.AddSetHeight), contentAlignment = Alignment.CenterStart) {
            AppTertiaryButton(
                onClick = onClick,
                text = "Add set",
                contentPadding = PaddingValues(horizontal = Theme.spacing.large),
            )
        }
    }
}

private enum class CapsuleStyle {
    Upcoming,
    Current,
    Done,
    Editing,
}

@Composable
private fun RoundRow(
    item: SpineItem.Round,
    state: WorkoutPrototypeState,
    actions: SpineActions,
    swipeEnabled: Boolean,
    onRestHeight: (Dp) -> Unit,
) {
    val round = item.round
    val isCurrent = state.current?.roundId == round.id
    val isEditing = state.editing?.ref?.roundId == round.id
    val style = when {
        isEditing -> CapsuleStyle.Editing
        isCurrent -> CapsuleStyle.Current
        round.done -> CapsuleStyle.Done
        else -> CapsuleStyle.Upcoming
    }
    val nodeStyle = when (style) {
        CapsuleStyle.Upcoming -> NodeStyle.Upcoming
        CapsuleStyle.Current -> NodeStyle.Current
        CapsuleStyle.Done -> NodeStyle.Done
        // White, like the dock's outline while it edits the set
        CapsuleStyle.Editing -> NodeStyle.Editing
    }
    val density = LocalDensity.current
    // The exercise's sets make one card, like an AppListGroup: the first set has its top corners
    // (unless rest's marker is above it), the last its bottom ones, and a divider between sets
    val resting = isCurrent && state.rest != null
    val first = item.number == 1
    val last = item.number == item.block.rounds.size
    val shape = when {
        first && !resting && last -> Theme.shapes.groupSingle
        first && !resting -> Theme.shapes.groupStart
        last -> Theme.shapes.groupEnd
        else -> Theme.shapes.groupMiddle
    }
    Column {
        AnimatedVisibility(
            modifier = Modifier.onSizeChanged { onRestHeight(with(density) { it.height.toDp() }) },
            visible = resting,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            RestRow(state.restRemainingSeconds(), first = first)
        }
        GutterRow(
            // 4dp after the card, before Add set, whose own height spaces it from the card too
            modifier = Modifier.padding(bottom = if (last) Theme.spacing.extraSmall else 0.dp),
            node = { SetNode(style = nodeStyle, onClick = { actions.onToggleRound(item.block.id, round.id) }) },
            nodeSize = WorkoutSizes.NodeTarget,
            centre = SpineSizes.CapsuleHeight / 2,
        ) {
            Column {
                SwipeToDelete(shape = shape, enabled = swipeEnabled, onDelete = { actions.onDeleteRound(item.block.id, round.id) }) {
                    Capsule(item, state, style, shape, actions)
                }
                if (!last) {
                    // On the card's background, so the divider doesn't show the page through
                    Box(modifier = Modifier.fillMaxWidth().background(Theme.backgrounds.surface)) {
                        HorizontalDivider(thickness = 1.dp, color = Theme.backgrounds.separator)
                    }
                }
            }
        }
    }
}

// Swiping a set to the left deletes it, any time
@Composable
private fun SwipeToDelete(shape: Shape, enabled: Boolean, onDelete: () -> Unit, content: @Composable () -> Unit) {
    val delete by rememberUpdatedState(onDelete)
    val swipeState = rememberSwipeToDismissBoxState()
    LaunchedEffect(swipeState.currentValue) {
        if (swipeState.currentValue == SwipeToDismissBoxValue.EndToStart) delete()
    }
    SwipeToDismissBox(
        state = swipeState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        gesturesEnabled = enabled,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Theme.contentColors.destructive.copy(alpha = 0.2f))
                    .padding(horizontal = Theme.spacing.large),
                contentAlignment = Alignment.CenterEnd,
            ) {
                AppText(
                    text = "Delete",
                    style = Theme.typography.caption.copy(fontWeight = FontWeight.ExtraBold),
                    color = Theme.contentColors.destructive,
                )
            }
        },
    ) {
        content()
    }
}

// The set's rows, as list rows (AppListItem), its part of the exercise's card. The current set is
// tinted lime, the one being edited raised. A superset or circuit's set has a row per exercise.
@Composable
private fun Capsule(item: SpineItem.Round, state: WorkoutPrototypeState, style: CapsuleStyle, shape: Shape, actions: SpineActions) {
    val tint = when (style) {
        CapsuleStyle.Current -> Theme.backgrounds.selected
        CapsuleStyle.Editing -> Theme.backgrounds.surfaceRaised
        else -> Color.Transparent
    }
    val round = item.round
    // A superset or circuit's exercises are named only in the round being done or edited. The other
    // rounds show just the values, in the same order as the title, to keep them short.
    val named = round.sets.size > 1 && (style == CapsuleStyle.Current || style == CapsuleStyle.Editing)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Theme.backgrounds.surface)
            .background(tint),
    ) {
        if (round.sets.size > 1 && !named) {
            CompactRound(item, state, actions)
            return@Column
        }
        round.sets.forEachIndexed { i, set ->
            if (set.removed) return@forEachIndexed
            if (i > 0) HorizontalDivider(thickness = 1.dp, color = Theme.backgrounds.separator)
            val ref = SetRef(item.block.id, round.id, i)
            val values = shownValues(state, ref, set)
            if (named) {
                GroupedSetRow(
                    name = item.block.exercises.getOrNull(i)?.name.orEmpty(),
                    set = set,
                    values = values,
                    active = state.current == ref,
                    editing = state.editing?.ref == ref,
                    onClick = { actions.onSetTapped(ref) },
                )
            } else {
                AppListItem(
                    title = values.summary(),
                    trailing = { SetTrailing(set, values) },
                    onClick = { actions.onSetTapped(ref) },
                )
            }
        }
    }
}

// A superset or circuit's round that isn't being done: its values as lines of one row, in the
// title's order, with the row's padding around them all rather than each. Tapping it picks the round:
// its first exercise not done (or the first, once all are).
@Composable
private fun CompactRound(item: SpineItem.Round, state: WorkoutPrototypeState, actions: SpineActions) {
    val round = item.round
    val target = round.sets.indexOfFirst { !it.done && !it.removed }.coerceAtLeast(0)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { actions.onSetTapped(SetRef(item.block.id, round.id, target)) }
            .padding(Theme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        round.sets.forEachIndexed { i, set ->
            if (set.removed) return@forEachIndexed
            val values = shownValues(state, SetRef(item.block.id, round.id, i), set)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                AppText(modifier = Modifier.weight(1f), text = values.summary(), style = Theme.typography.body)
                SetTrailing(set, values)
            }
        }
    }
}

// One exercise's set in a superset or circuit, laid out like AppListItem with the exercise as its
// header. The one you're on has a lime dot and name, the one being edited a white one.
@Composable
private fun GroupedSetRow(name: String, set: PrototypeSet, values: SetValues, active: Boolean, editing: Boolean, onClick: () -> Unit) {
    val marker = when {
        editing -> WorkoutColors.Editing
        active -> Theme.contentColors.action
        else -> null
    }
    AppListRow(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                    marker?.let { SpineDot(it, size = WorkoutSizes.ActiveDot) }
                    AppText(
                        text = name,
                        style = Theme.typography.caption,
                        color = marker ?: Theme.contentColors.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AppText(text = values.summary(), style = Theme.typography.body)
            }
            SetTrailing(set, values)
        }
    }
}

// A set being edited shows the dock's values as they change
private fun shownValues(state: WorkoutPrototypeState, ref: SetRef, set: PrototypeSet): SetValues =
    state.editing?.takeIf { it.ref == ref }?.values ?: set.values

// The change chips once done, then last time's values
@Composable
private fun SetTrailing(set: PrototypeSet, values: SetValues) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
        if (set.done) {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
                changes(values, set.lastTime).forEach { (text, up) -> ChangeChip(text = text, up = up) }
            }
        }
        set.lastTime?.let {
            AppText(
                text = it.summary(),
                style = Theme.typography.caption.copy(fontFeatureSettings = "tnum"),
                color = Theme.contentColors.secondary,
                maxLines = 1,
            )
        }
    }
}
