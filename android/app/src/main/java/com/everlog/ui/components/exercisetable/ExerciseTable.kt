package com.everlog.ui.components.exercisetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.everlog.R
import com.everlog.android.common.compose.elements.AppIcon
import com.everlog.android.common.compose.elements.AppMenuItem
import com.everlog.android.common.compose.elements.AppOverflowMenu
import com.everlog.android.common.compose.elements.AppTertiaryButton
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.AppThumbnail
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.utils.format.EmptyValue
import com.everlog.utils.format.getTitle
import com.everlog.android.common.design.R as DesignR
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object TableSizes {
    val ThumbnailCorner = 12.dp
    val GroupRowGap = 6.dp
    val RowHeight = 52.dp
    val DeleteWidth = 88.dp
    // The suggestion mark beside a weight, smaller than any text style
    val Mark = 9.sp
    val MarkGap = 2.dp
    val RowPadding = 12.dp
    val SetColumn = 32.dp
    val Cell = 72.dp
    val CheckDone = 32.dp
    val CheckTarget = 44.dp
    val ColumnGap = 8.dp
    val FocusOutline = 1.5.dp
    val CheckIcon = 18.dp
    const val KeypadOpenMillis = 350L
}

/**
 * Per exercise its header, then a card of rows: SET · LAST TIME · REPS (or TIME) · KG · tick. A superset or circuit
 * has a row per exercise in each round, in its header's order.
 */
@Composable
internal fun ExerciseTable(
    blocks: List<WorkoutBlock>,
    view: TableView,
    // Goes up with every keypad key, to bring the edited cell back into view
    reveal: Int,
    contentPadding: PaddingValues,
    actions: ExerciseTableActions,
    // Scrolls with the table, above its first exercise
    header: (@Composable () -> Unit)? = null,
) {
    // The edited cell is kept below the table's top and above the keypad, which floats over the table
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
    LaunchedEffect(view.focus?.ref?.blockId, reveal) {
        val blockId = view.focus?.ref?.blockId ?: return@LaunchedEffect
        val block = blocks.indexOfFirst { it.id == blockId }
        // The header is the list's first item
        val index = block + if (header != null) 1 else 0
        if (block >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            listState.animateScrollToItem(index)
        }
    }
    val openSwipe = remember { mutableStateOf<SetRef?>(null) }
    CompositionLocalProvider(LocalEnsureVisible provides ensureVisible, LocalReveal provides reveal, LocalOpenSwipe provides openSwipe) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(ExerciseTableTags.Table)
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
            if (header != null) item(key = "header") { header() }
            blocks.forEachIndexed { index, block ->
                item(key = block.id) {
                    BlockTable(index = index, block = block, view = view, actions = actions)
                }
            }
        }
    }
}

// Scrolls the table so a cell at these window positions (top, bottom) is clear of the keypad
private val LocalEnsureVisible = staticCompositionLocalOf<suspend (Float, Float) -> Unit> { { _, _ -> } }

private val LocalReveal = staticCompositionLocalOf { 0 }

// The set whose Delete is showing: one at a time, so opening another closes it
private val LocalOpenSwipe = staticCompositionLocalOf { mutableStateOf<SetRef?>(null) }

@Composable
private fun BlockTable(
    // For the rows' test tags
    index: Int,
    block: WorkoutBlock,
    view: TableView,
    actions: ExerciseTableActions,
) {
    Column(modifier = Modifier.padding(bottom = Theme.spacing.extraLarge)) {
        ExerciseHeader(index, block, actions)
        Spacer(modifier = Modifier.height(Theme.spacing.medium))
        ColumnLabels(block, view.unitLabel)
        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Theme.shapes.surface)
                .background(Theme.backgrounds.surface),
        ) {
            block.rows(view).forEach { row ->
                if (row.round > 0 && row.startsRound) {
                    HorizontalDivider(thickness = 1.dp, color = Theme.backgrounds.separator)
                }
                // Keyed, so the next set doesn't take over a swiped one's state once it's gone
                key(row.ref.roundId, row.exercise) {
                    val tag = ExerciseTableTags.set(index, row.round, row.exercise)
                    SwipeToDelete(ref = row.ref, tag = tag, onDelete = { actions.onDeleteSet(row.ref) }) {
                        TableRow(tag = tag, row = row, actions = actions)
                    }
                }
            }
        }
        AppTertiaryButton(
            modifier = Modifier.align(Alignment.CenterHorizontally).testTag(ExerciseTableTags.addSet(index)),
            onClick = { actions.onAddSet(block.id) },
            text = stringResource(R.string.workout_log_add_set),
        )
    }
}

private enum class SwipeReveal { Closed, Open }

// Swiping a set to the left uncovers its Delete button, so a stray swipe can't delete it on its own
@Composable
private fun SwipeToDelete(ref: SetRef, tag: String, onDelete: () -> Unit, content: @Composable () -> Unit) {
    val revealPx = with(LocalDensity.current) { TableSizes.DeleteWidth.toPx() }
    val state = remember { AnchoredDraggableState(SwipeReveal.Closed) }
    SideEffect {
        state.updateAnchors(DraggableAnchors { SwipeReveal.Closed at 0f; SwipeReveal.Open at -revealPx })
    }
    var openSwipe by LocalOpenSwipe.current
    LaunchedEffect(state) {
        snapshotFlow { state.targetValue }.collect { if (it == SwipeReveal.Open) openSwipe = ref }
    }
    LaunchedEffect(openSwipe) {
        if (openSwipe != ref && state.targetValue == SwipeReveal.Open) state.animateTo(SwipeReveal.Closed)
    }
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(TableSizes.DeleteWidth)
                .background(Theme.contentColors.destructive.copy(alpha = 0.2f))
                .testTag(ExerciseTableTags.delete(tag))
                .clickable {
                    onDelete()
                    scope.launch { state.snapTo(SwipeReveal.Closed) }
                },
            contentAlignment = Alignment.Center,
        ) {
            AppText(text = stringResource(R.string.workout_log_delete), style = Theme.typography.bodyBold, color = Theme.contentColors.destructive)
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(state.offset.takeIf { !it.isNaN() }?.roundToInt() ?: 0, 0) }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(state, positionalThreshold = { it / 2 }),
                )
                // So the red behind only shows as it's swiped
                .background(Theme.backgrounds.surface),
        ) {
            content()
        }
    }
}

@Composable
private fun ExerciseHeader(index: Int, block: WorkoutBlock, actions: ExerciseTableActions) {
    Row(
        modifier = Modifier.padding(horizontal = TableSizes.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        if (block.exercises.size == 1) {
            val exercise = block.exercises.first().exercise
            Row(
                modifier = Modifier.weight(1f).openingInfo(exercise, actions, TableSizes.ThumbnailCorner),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
            ) {
                AppThumbnail(
                    imageUrl = exercise?.imageUrl,
                    size = Theme.sizes.leading,
                    corner = TableSizes.ThumbnailCorner,
                )
                AppText(
                    text = exercise?.name.orEmpty(),
                    style = Theme.typography.bodyBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            GroupHeader(modifier = Modifier.weight(1f).alignBy(LastExerciseCenter), block = block, actions = actions)
        }
        val exercises = block.exercises.mapNotNull { it.exercise }
        AppOverflowMenu(
            // Beside a group's last exercise, as it's under the group's type
            modifier = Modifier.testTag(ExerciseTableTags.menu(index)).then(
                if (block.exercises.size > 1) Modifier.alignBy { it.measuredHeight / 2 } else Modifier,
            ),
            outlined = true,
            items = exercises.map { exercise ->
                val text = if (exercises.size == 1) {
                    stringResource(R.string.workout_log_history)
                } else {
                    stringResource(R.string.workout_log_exercise_history, exercise.name.orEmpty())
                }
                AppMenuItem(text = text, onClick = { actions.onOpenExercise(exercise, ExerciseTab.History) })
            } + AppMenuItem(text = stringResource(R.string.workout_log_delete), onClick = { actions.onDeleteExercise(block.id) }),
        )
    }
}

// The press shows with the thumbnail's corners, which a rounder shape would cut into
private fun Modifier.openingInfo(exercise: ELExercise?, actions: ExerciseTableActions, corner: Dp) =
    if (exercise == null) this else clip(RoundedCornerShape(corner)).clickable { actions.onOpenExercise(exercise, ExerciseTab.Info) }

// The middle of a group header's last exercise, which the exercise menu lines up with
private val LastExerciseCenter = HorizontalAlignmentLine(merger = { _, last -> last })

private fun Modifier.providingCenter(line: HorizontalAlignmentLine) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height, mapOf(line to placeable.height / 2)) { placeable.place(0, 0) }
}

// SUPERSET, CIRCUIT and so on over the names, each with its image and letter
@Composable
private fun GroupHeader(modifier: Modifier, block: WorkoutBlock, actions: ExerciseTableActions) {
    val context = LocalContext.current
    val type = block.setType
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(TableSizes.GroupRowGap)) {
        type?.getTitle(context)?.let { title ->
            AppText(
                modifier = Modifier.padding(start = GroupThumbnailSizes.Size + Theme.spacing.medium),
                text = title.uppercase(),
                style = Theme.typography.small,
                color = Theme.contentColors.secondary,
            )
        }
        block.exercises.forEachIndexed { i, exercise ->
            Row(
                modifier = Modifier
                    .then(if (i == block.exercises.lastIndex) Modifier.providingCenter(LastExerciseCenter) else Modifier)
                    .openingInfo(exercise.exercise, actions, GroupThumbnailSizes.Corner),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
            ) {
                GroupThumbnail(exercise.exercise?.imageUrl, 'a' + i)
                AppText(
                    text = exercise.exercise?.name.orEmpty(),
                    style = Theme.typography.bodyBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ColumnLabels(block: WorkoutBlock, unitLabel: String) {
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
        Label(stringResource(R.string.workout_log_column_last_time), Modifier.weight(1f))
        Label(count, Modifier.width(TableSizes.Cell), TextAlign.Center)
        Label(unitLabel, Modifier.width(TableSizes.Cell), TextAlign.Center)
        Spacer(modifier = Modifier.width(TableSizes.CheckTarget))
    }
}

@Composable
private fun Label(text: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    AppText(modifier = modifier, text = text, style = Theme.typography.small, color = Theme.contentColors.secondary, textAlign = align, maxLines = 1)
}

// The selected row is tinted and shows its values as boxes to type into
@Composable
private fun TableRow(tag: String, row: SetRow, actions: ExerciseTableActions) {
    val onCount = { actions.onCellTapped(CellFocus(row.ref, row.countField)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TableSizes.RowHeight)
            .testTag(tag)
            .background(if (row.selected) Theme.backgrounds.selected else Color.Transparent)
            .clickable(onClick = onCount)
            .padding(horizontal = TableSizes.RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TableSizes.ColumnGap),
    ) {
        AppText(
            modifier = Modifier.width(TableSizes.SetColumn),
            textAlign = TextAlign.Center,
            text = row.number.orEmpty(),
            style = Theme.typography.bodyBold,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            AppText(
                text = row.lastTime ?: EmptyValue,
                style = Theme.typography.caption.copy(fontFeatureSettings = "tnum"),
                color = Theme.contentColors.secondary,
                maxLines = 1,
            )
        }
        // Reps (or time) first, then weight, as last time reads: 15 × 17 kg
        Cell(
            tag = ExerciseTableTags.count(tag),
            text = row.countText,
            focused = row.countFocused,
            boxed = row.selected,
            onClick = onCount,
            highlight = row.timing,
            suggested = row.countSuggested,
        )
        Cell(
            tag = ExerciseTableTags.weight(tag),
            text = row.weightText,
            focused = row.weightFocused,
            boxed = row.selected,
            onClick = { actions.onCellTapped(CellFocus(row.ref, ValueField.Weight)) },
            suggested = row.weightSuggested,
        )
        when {
            row.startsTimer -> StartButton(tag = ExerciseTableTags.tick(tag), armed = row.armed, onClick = { actions.onStartTimer(row.ref) })
            else -> Tick(tag = ExerciseTableTags.tick(tag), done = row.done, armed = row.armed, onClick = { actions.onTick(row.ref) })
        }
    }
}

// Boxed on the selected row, plain text on the others
@Composable
private fun Cell(
    tag: String,
    text: String,
    focused: Boolean,
    boxed: Boolean,
    onClick: () -> Unit,
    highlight: Boolean = false,
    suggested: Boolean = false,
) {
    val shape = Theme.shapes.button
    // The whole row's height takes the tap; the press shows on the box
    val interactions = remember { MutableInteractionSource() }
    var top by remember { mutableStateOf(0f) }
    var bottom by remember { mutableStateOf(0f) }
    val ensureVisible by rememberUpdatedState(LocalEnsureVisible.current)
    // After the keypad has opened, so the cell ends up above it
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
                .size(TableSizes.Cell, Theme.sizes.rowControl)
                .clip(shape)
                .indication(interactions, ripple())
                .then(
                    if (boxed) {
                        Modifier
                            .background(Theme.backgrounds.inset)
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
                if (suggested) {
                    AppText(
                        modifier = Modifier.padding(start = TableSizes.MarkGap),
                        text = SuggestedMark,
                        style = Theme.typography.small.copy(fontSize = TableSizes.Mark),
                        color = Theme.contentColors.action,
                    )
                }
            }
        }
    }
}

@Composable
private fun StartButton(tag: String, armed: Boolean, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(TableSizes.CheckTarget, TableSizes.RowHeight)
            .testTag(tag)
            .clickable(interactionSource = interactions, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(Theme.sizes.rowControl)
                .clip(CircleShape)
                .indication(interactions, ripple())
                .background(Theme.backgrounds.inset)
                .border(
                    if (armed) TableSizes.FocusOutline else 1.dp,
                    if (armed) Theme.contentColors.action else Theme.backgrounds.separator,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                modifier = Modifier.size(TableSizes.CheckIcon),
                imageVector = ImageVector.vectorResource(DesignR.drawable.ic_timer),
                tint = if (armed) Theme.contentColors.action else Theme.contentColors.secondary,
                contentDescription = stringResource(R.string.workout_log_start_timer),
            )
        }
    }
}

@Composable
private fun Tick(tag: String, done: Boolean, armed: Boolean, onClick: () -> Unit) {
    val shape = CircleShape
    // The tap area is the row's height and wider than the tick, but the press shows on the tick
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
                .size(if (done) TableSizes.CheckDone else Theme.sizes.rowControl)
                .clip(shape)
                .indication(interactions, ripple())
                .background(if (done) Theme.contentColors.action else Theme.backgrounds.inset)
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
                imageVector = ImageVector.vectorResource(DesignR.drawable.ic_check),
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

internal const val SuggestedMark = "✦"

/**
 * Rows are tagged by position: the exercise group, the round and the exercise in it.
 */
internal object ExerciseTableTags {
    const val Table = "table"
    const val EditList = "edit"

    fun editCard(block: Int) = "edit:$block"

    fun editGrip(block: Int) = "edit:$block:grip"

    fun set(block: Int, round: Int, exercise: Int) = "set:$block:$round:$exercise"

    fun menu(block: Int) = "menu:$block"

    fun addSet(block: Int) = "add_set:$block"

    fun delete(set: String) = "$set:delete"

    fun count(set: String) = "$set:count"

    fun weight(set: String) = "$set:weight"

    fun tick(set: String) = "$set:tick"
}
