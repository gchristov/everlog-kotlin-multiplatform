package com.everlog.ui.components.exercisetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.everlog.R
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme

private object EditSizes {
    val CardGap = 12.dp
    val LiftScale = 1.03f
    val RowPadding = 12.dp
    // Centres the bin in a single exercise's row
    val BinPadding = 2.dp
    val GroupRowGap = 6.dp
}

/**
 * The exercises as cards to drag into a new order by their grip (a superset moves as one), or delete with the bin,
 * which asks first.
 */
@Composable
internal fun ExerciseEditList(
    blocks: List<WorkoutBlock>,
    contentPadding: PaddingValues,
    onMove: (from: Int, to: Int) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val current by rememberUpdatedState(blocks)
    val heights = remember { mutableStateMapOf<Long, Int>() }
    var dragging by remember { mutableStateOf<Long?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val gap = with(LocalDensity.current) { EditSizes.CardGap.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ExerciseTableTags.EditList)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Theme.spacing.large)
            .padding(top = Theme.spacing.medium, bottom = contentPadding.calculateBottomPadding() + Theme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(EditSizes.CardGap),
    ) {
        blocks.forEachIndexed { index, block ->
            key(block.id) {
                val lifted = dragging == block.id
                EditCard(
                    modifier = Modifier
                        .testTag(ExerciseTableTags.editCard(index))
                        .onSizeChanged { heights[block.id] = it.height }
                        .zIndex(if (lifted) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (lifted) offset else 0f
                            val scale = if (lifted) EditSizes.LiftScale else 1f
                            scaleX = scale
                            scaleY = scale
                        },
                    block = block,
                    lifted = lifted,
                    gripModifier = Modifier
                        .testTag(ExerciseTableTags.editGrip(index))
                        .pointerInput(block.id) {
                            detectDragGestures(
                                onDragStart = {
                                    dragging = block.id
                                    offset = 0f
                                },
                                onDragEnd = { dragging = null; offset = 0f },
                                onDragCancel = { dragging = null; offset = 0f },
                                onDrag = { change, amount ->
                                    change.consume()
                                    offset += amount.y
                                    // Past half of the neighbour's height, the two swap places
                                    val at = current.indexOfFirst { it.id == block.id }
                                    if (offset > 0 && at < current.lastIndex) {
                                        val step = (heights[current[at + 1].id] ?: 0) + gap
                                        if (offset > step / 2) {
                                            onMove(at, at + 1)
                                            offset -= step
                                        }
                                    } else if (offset < 0 && at > 0) {
                                        val step = (heights[current[at - 1].id] ?: 0) + gap
                                        if (-offset > step / 2) {
                                            onMove(at, at - 1)
                                            offset += step
                                        }
                                    }
                                },
                            )
                        },
                    onDelete = { onDelete(block.id) },
                )
            }
        }
    }
}

// The grip, outside the card on its left, beside the last exercise. The card holds a group's type, the names and the
// bin; it's raised while dragged, the grip staying outside it.
@Composable
private fun EditCard(
    block: WorkoutBlock,
    lifted: Boolean,
    gripModifier: Modifier,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = Theme.shapes.surface
    val context = LocalContext.current
    val group = block.exercises.size > 1
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = gripModifier
                .alignBy { it.measuredHeight / 2 }
                .size(Theme.sizes.leading)
                .border(1.dp, Theme.backgrounds.separator, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_drag),
                tint = Theme.contentColors.secondary,
                contentDescription = stringResource(R.string.workout_log_move),
            )
        }
        Spacer(modifier = Modifier.width(Theme.spacing.large))
        Row(
            modifier = Modifier
                .weight(1f)
                .alignBy(LastExerciseCenter)
                .clip(shape)
                .background(if (lifted) Theme.backgrounds.surfaceRaised else Theme.backgrounds.surface)
                .then(if (lifted) Modifier.border(1.dp, Theme.contentColors.action, shape) else Modifier)
                // A single exercise's row is the grip's height, so the two line up
                .heightIn(min = Theme.sizes.leading)
                .padding(start = EditSizes.RowPadding, end = EditSizes.BinPadding, top = if (group) Theme.spacing.small else 0.dp, bottom = if (group) Theme.spacing.small else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = if (group) Arrangement.spacedBy(EditSizes.GroupRowGap) else Arrangement.Center,
            ) {
                block.setType?.takeIf { group }?.getTitle(context)?.let { type ->
                    AppText(text = type.uppercase(), style = Theme.typography.small, color = Theme.contentColors.secondary)
                }
                block.exercises.forEachIndexed { i, exercise ->
                    AppText(
                        modifier = if (i == block.exercises.lastIndex) Modifier.providingCenter(LastExerciseCenter) else Modifier,
                        text = exercise.exercise?.name.orEmpty(),
                        style = Theme.typography.bodyBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            InsetDeleteButton(onClick = onDelete, contentDescription = stringResource(R.string.workout_log_delete))
        }
    }
}

// The middle of the card's last exercise, which the grip lines up with
private val LastExerciseCenter = HorizontalAlignmentLine(merger = { _, last -> last })

private fun Modifier.providingCenter(line: HorizontalAlignmentLine) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height, mapOf(line to placeable.height / 2)) { placeable.place(0, 0) }
}
