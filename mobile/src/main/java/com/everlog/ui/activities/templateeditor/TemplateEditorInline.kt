package com.everlog.ui.activities.templateeditor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import com.everlog.ui.design.elements.AppIconButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.everlog.R
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.model.exercise.ELExercise
import com.everlog.ui.components.exercisetable.CellFocus
import com.everlog.ui.components.exercisetable.ExerciseTab
import com.everlog.ui.components.exercisetable.countField
import com.everlog.ui.components.exercisetable.typedSeconds
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppMenuItem
import com.everlog.ui.design.elements.AppOverflowMenu
import com.everlog.ui.design.elements.AppTertiaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.AppThumbnail
import com.everlog.ui.design.theme.Theme
import com.everlog.utils.format.formatClock

// Prototype: a template being built, with every control on screen rather than behind gestures. Each set's reps sit
// beside its number with a delete on the right; exercises drag by their grip, and the link between two joins them.
// Hardcoded English, not for release.

// Softer than the destructive red, for a bin on every row
private val Coral = Color(0xFFF07A6A)

private object InlineSizes {
    val Thumbnail = 40.dp
    val ThumbnailCorner = 12.dp
    val Grip = 40.dp
    // Lines the grip up with the exercise's name
    val GripTop = 12.dp
    val RowHeight = 52.dp
    val RowPadding = 12.dp
    val SetColumn = 32.dp
    val Cell = 72.dp
    val CellHeight = 36.dp
    val ColumnGap = 8.dp
    val FocusOutline = 1.5.dp
    val LiftScale = 1.02f
}

@Composable
internal fun InlineTemplateList(
    state: TemplateEditorState,
    viewModel: TemplateEditorViewModel,
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
    onOpenExercise: (ELExercise, ExerciseTab) -> Unit,
) {
    val current by rememberUpdatedState(state.blocks)
    val tops = remember { mutableStateMapOf<Long, Float>() }
    val heights = remember { mutableStateMapOf<Long, Int>() }
    var dragging by remember { mutableStateOf<Long?>(null) }
    // Where the dragged card was when the drag started and how far the finger has gone since, in the window
    var dragStart by remember { mutableFloatStateOf(0f) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val keepClear = with(LocalDensity.current) { (contentPadding.calculateBottomPadding() + Theme.spacing.extraLarge).toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Theme.spacing.large)
            .padding(top = Theme.spacing.medium, bottom = contentPadding.calculateBottomPadding() + Theme.spacing.large),
    ) {
        header()
        Spacer(modifier = Modifier.height(Theme.spacing.large))
        state.blocks.forEachIndexed { index, block ->
            key(block.id) {
                val lifted = dragging == block.id
                val grip = Modifier.pointerInput(block.id) {
                    detectDragGestures(
                        onDragStart = {
                            viewModel.onDragStarted()
                            dragStart = tops[block.id] ?: 0f
                            dragDistance = 0f
                            dragging = block.id
                        },
                        onDragEnd = { dragging = null },
                        onDragCancel = { dragging = null },
                        onDrag = { change, amount ->
                            change.consume()
                            dragDistance += amount.y
                            // Past the middle of a neighbour, the two swap places
                            val at = current.indexOfFirst { it.id == block.id }
                            val middle = dragStart + dragDistance + (heights[block.id] ?: 0) / 2f
                            val next = current.getOrNull(at + 1)
                            val previous = current.getOrNull(at - 1)
                            if (next != null && middle > (tops[next.id] ?: 0f) + (heights[next.id] ?: 0) / 2f) {
                                viewModel.onMoveExercise(at, at + 1)
                            } else if (previous != null && middle < (tops[previous.id] ?: 0f) + (heights[previous.id] ?: 0) / 2f) {
                                viewModel.onMoveExercise(at, at - 1)
                            }
                        },
                    )
                }
                Column(
                    modifier = Modifier
                        // Outside the lift, so these are where the card is laid out, not where it's drawn
                        .onGloballyPositioned { tops[block.id] = it.positionInWindow().y }
                        .onSizeChanged { heights[block.id] = it.height }
                        .zIndex(if (lifted) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (lifted) dragStart + dragDistance - (tops[block.id] ?: 0f) else 0f
                            val scale = if (lifted) InlineSizes.LiftScale else 1f
                            scaleX = scale
                            scaleY = scale
                        },
                ) {
                Row(verticalAlignment = if (dragging == null) Alignment.Top else Alignment.CenterVertically) {
                    Box(modifier = grip.padding(top = if (dragging == null) InlineSizes.GripTop else 0.dp).size(InlineSizes.Grip), contentAlignment = Alignment.Center) {
                        AppIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_drag), tint = Theme.contentColors.secondary, contentDescription = "Move")
                    }
                    ExerciseCard(
                    modifier = Modifier.weight(1f),
                    block = block,
                    state = state,
                    viewModel = viewModel,
                    collapsed = dragging != null,
                    lifted = lifted,
                    keepClear = keepClear,
                    onOpenExercise = onOpenExercise,
                )
                }
                if (dragging == null) {
                    val next = state.blocks.getOrNull(index + 1)
                    BlockActions(
                        onAddSet = { viewModel.onAddSet(block.id) },
                        addSet = if (block.exercises.size > 1) "Add round" else "Add set",
                        join = next?.let { if (block.exercises.size + it.exercises.size == 2) "Superset with next" else "Circuit with next" },
                        onJoin = { viewModel.onJoinWithNext(block.id) },
                    )
                } else {
                    Spacer(modifier = Modifier.height(Theme.spacing.medium))
                }
                }
            }
        }
    }
}

// Under an exercise, as the workout's Add set: adding a set, and joining it with the exercise after it
@Composable
private fun BlockActions(onAddSet: () -> Unit, addSet: String, join: String?, onJoin: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = InlineSizes.Grip, bottom = Theme.spacing.small),
        horizontalArrangement = Arrangement.Center,
    ) {
        AppTertiaryButton(onClick = onAddSet, text = addSet)
        if (join != null) AppTertiaryButton(onClick = onJoin, text = join)
    }
}

@Composable
private fun ExerciseCard(
    modifier: Modifier,
    block: WorkoutBlock,
    state: TemplateEditorState,
    viewModel: TemplateEditorViewModel,
    // While any card is dragged, every card is its header only, so it's a short way to anywhere
    collapsed: Boolean,
    lifted: Boolean,
    keepClear: Float,
    onOpenExercise: (ELExercise, ExerciseTab) -> Unit,
) {
    val shape = Theme.shapes.surface
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (lifted) Theme.backgrounds.surfaceRaised else Theme.backgrounds.surface)
            .border(1.dp, if (lifted) Theme.contentColors.action else Theme.backgrounds.separator, shape),
    ) {
        CardHeader(block, viewModel, collapsed, onOpenExercise)
        if (collapsed) return@Column
        ColumnLabels(block)
        block.rounds.forEachIndexed { r, round ->
            round.sets.forEachIndexed { i, set ->
                if (set.removed) return@forEachIndexed
                key(round.id, i) {
                    HorizontalDivider(thickness = 1.dp, color = Theme.backgrounds.separator)
                    val number = if (round.sets.size > 1) "${r + 1}${'a' + i}" else "${r + 1}"
                    SetRow(SetRef(block.id, round.id, i), number, set, state, viewModel, keepClear)
                }
            }
        }
    }
}

@Composable
private fun CardHeader(
    block: WorkoutBlock,
    viewModel: TemplateEditorViewModel,
    collapsed: Boolean,
    onOpenExercise: (ELExercise, ExerciseTab) -> Unit,
) {
    val context = LocalContext.current
    val group = block.exercises.size > 1
    Row(
        modifier = Modifier.padding(start = Theme.spacing.medium, end = Theme.spacing.small, top = Theme.spacing.small, bottom = Theme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            if (group) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(
                        modifier = Modifier
                            .clip(Theme.shapes.button)
                            .clickable { viewModel.onGroupTypeTapped(block.id) }
                            .padding(vertical = Theme.spacing.extraSmall),
                        text = "${block.setType?.getTitle(context)?.uppercase().orEmpty()} ▾",
                        style = Theme.typography.small,
                        color = Theme.contentColors.secondary,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (!collapsed) {
                        AppText(
                            modifier = Modifier
                                .clip(Theme.shapes.button)
                                .clickable { viewModel.onSplitGroup(block.id) }
                                .padding(horizontal = Theme.spacing.small, vertical = Theme.spacing.extraSmall),
                            text = "Split",
                            style = Theme.typography.small,
                            color = Theme.contentColors.action,
                        )
                    }
                }
            }
            block.exercises.forEachIndexed { i, exercise ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(InlineSizes.ThumbnailCorner))
                        .clickable(enabled = exercise.exercise != null) { exercise.exercise?.let { onOpenExercise(it, ExerciseTab.Info) } },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
                ) {
                    if (!collapsed) AppThumbnail(imageUrl = exercise.exercise?.imageUrl, size = InlineSizes.Thumbnail, corner = InlineSizes.ThumbnailCorner)
                    AppText(
                        text = (if (group) "${'a' + i}  " else "") + exercise.exercise?.name.orEmpty(),
                        style = Theme.typography.bodyBold,
                        maxLines = if (collapsed) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (!collapsed) {
            val exercises = block.exercises.mapNotNull { it.exercise }
            AppOverflowMenu(
                items = exercises.map { exercise ->
                    AppMenuItem(text = if (exercises.size == 1) "History" else "History: ${exercise.name.orEmpty()}", onClick = { onOpenExercise(exercise, ExerciseTab.History) })
                } + AppMenuItem(text = if (block.exercises.size > 1) "Delete group" else "Delete exercise", onClick = { viewModel.onDeleteExercise(block.id) }),
            )
        }
    }
}

@Composable
private fun ColumnLabels(block: WorkoutBlock) {
    val modes = block.rounds.flatMap { round -> round.sets.filterNot { it.removed }.map { it.mode } }.toSet()
    val count = when {
        modes == setOf(SetMode.Time) -> "TIME"
        SetMode.Time in modes -> "REPS/TIME"
        else -> "REPS"
    }
    Row(
        modifier = Modifier.padding(horizontal = InlineSizes.RowPadding, vertical = Theme.spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(InlineSizes.ColumnGap),
    ) {
        Label("SET", Modifier.width(InlineSizes.SetColumn))
        Label(count, Modifier.width(InlineSizes.Cell))
    }
}

@Composable
private fun Label(text: String, modifier: Modifier) {
    AppText(modifier = modifier, text = text, style = Theme.typography.small, color = Theme.contentColors.secondary, textAlign = TextAlign.Center, maxLines = 1)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetRow(ref: SetRef, number: String, set: WorkoutSet, state: TemplateEditorState, viewModel: TemplateEditorViewModel, keepClear: Float) {
    val cell = CellFocus(ref, countField(set))
    val focused = state.focus == cell
    val selected = state.focus?.ref == ref
    val requester = remember { BringIntoViewRequester() }
    var height by remember { mutableIntStateOf(0) }
    // Clear of the keypad, which floats over the list
    LaunchedEffect(focused, state.reveal) {
        if (focused) requester.bringIntoView(Rect(0f, 0f, 1f, height + keepClear))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(InlineSizes.RowHeight)
            .onSizeChanged { height = it.height }
            .bringIntoViewRequester(requester)
            .background(if (selected) Theme.backgrounds.selected else Theme.backgrounds.surface)
            .padding(start = InlineSizes.RowPadding, end = Theme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InlineSizes.ColumnGap),
    ) {
        AppText(modifier = Modifier.width(InlineSizes.SetColumn), text = number, color = Theme.contentColors.secondary, textAlign = TextAlign.Center)
        ValueCell(text = valueText(state, cell, set), selected = selected, focused = focused) { viewModel.onCellTapped(cell) }
        Spacer(modifier = Modifier.weight(1f))
        AppIconButton(
            onClick = { viewModel.onDeleteSet(ref) },
            icon = ImageVector.vectorResource(R.drawable.ic_delete),
            tint = Coral,
            contentDescription = "Delete set",
            outlined = true,
        )
    }
}
private fun valueText(state: TemplateEditorState, cell: CellFocus, set: WorkoutSet): String {
    val time = set.mode == SetMode.Time
    val typed = state.typed?.takeIf { state.focus == cell }
    if (typed == "") return ""
    val value = if (time) typed?.let { typedSeconds(it) } ?: set.timeSeconds else typed?.toIntOrNull() ?: set.reps
    return when {
        value == null -> "–"
        time -> formatClock(value.toLong())
        else -> value.toString()
    }
}

// As the workout table's cell: the value, boxed on the selected row
@Composable
private fun ValueCell(text: String, selected: Boolean, focused: Boolean, onClick: () -> Unit) {
    val shape = Theme.shapes.button
    Box(
        modifier = Modifier
            .width(InlineSizes.Cell)
            .height(InlineSizes.CellHeight)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(Theme.backgrounds.inset)
                        .border(if (focused) InlineSizes.FocusOutline else 1.dp, if (focused) Theme.contentColors.action else Theme.backgrounds.separator, shape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = text, style = Theme.typography.body.copy(fontFeatureSettings = "tnum"), maxLines = 1)
    }
}
