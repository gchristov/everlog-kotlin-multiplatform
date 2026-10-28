package com.everlog.ui.components.exercisetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.everlog.R
import com.everlog.data.controllers.workoutsession.ExerciseRef
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.ui.design.elements.AppCheck
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme

private object EditSizes {
    val CardGap = 8.dp
    // Bigger than the icon, so the grip is easy to catch
    val Grip = 40.dp
    val LiftScale = 1.03f
    // A ticked exercise's row, the tap target's height
    val PickRow = 48.dp
    // Lines the type and sets up with the names, past the tick
    val PickRowStart = 44.dp
}

/**
 * The exercises as cards to drag into a new order by their grip (a superset moves as one), or delete with the bin,
 * which asks first.
 *
 * @param picked With a set, each exercise has a tick in place of the bin, e.g. to group or delete several at once
 */
@Composable
internal fun ExerciseEditList(
    blocks: List<WorkoutBlock>,
    contentPadding: PaddingValues,
    onMove: (from: Int, to: Int) -> Unit,
    onDelete: (Long) -> Unit,
    picked: Set<ExerciseRef>? = null,
    onTogglePicked: (ExerciseRef) -> Unit = {},
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
                val cardModifier = Modifier
                    .testTag(ExerciseTableTags.editCard(index))
                    .onSizeChanged { heights[block.id] = it.height }
                    .zIndex(if (lifted) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (lifted) offset else 0f
                        val scale = if (lifted) EditSizes.LiftScale else 1f
                        scaleX = scale
                        scaleY = scale
                    }
                val gripModifier = Modifier
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
                    }
                if (picked == null) {
                    EditCard(block, lifted, gripModifier, onDelete = { onDelete(block.id) }, modifier = cardModifier)
                } else {
                    PickCard(index, block, lifted, gripModifier, picked, onTogglePicked, modifier = cardModifier)
                }
            }
        }
    }
}

// An exercise, or a group's type over its exercises, with its number of sets, the grip and the bin
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
    AppListItem(
        modifier = modifier
            .clip(shape)
            .background(if (lifted) Theme.backgrounds.surfaceRaised else Theme.backgrounds.surface)
            .border(1.dp, if (lifted) Theme.contentColors.action else Theme.backgrounds.separator, shape),
        header = block.setType?.takeIf { block.exercises.size > 1 }?.getTitle(context),
        // One exercise per line, like the table's headers
        title = block.exercises.joinToString("\n") { it.exercise?.name.orEmpty() },
        subtitle = pluralStringResource(R.plurals.workout_log_sets, block.rounds.size, block.rounds.size),
        leading = {
            Box(modifier = gripModifier.size(EditSizes.Grip), contentAlignment = Alignment.Center) {
                AppIcon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_drag),
                    tint = Theme.contentColors.secondary,
                    contentDescription = stringResource(R.string.workout_log_move),
                )
            }
        },
        trailing = {
            AppIconButton(
                onClick = onDelete,
                icon = ImageVector.vectorResource(R.drawable.ic_delete),
                tint = Theme.contentColors.destructive,
                contentDescription = stringResource(R.string.workout_log_delete),
                outlined = true,
            )
        },
    )
}

// A group's type over its exercises, each with a tick, then the number of sets, with the grip on the right
@Composable
private fun PickCard(
    // For the ticks' test tags
    index: Int,
    block: WorkoutBlock,
    lifted: Boolean,
    gripModifier: Modifier,
    picked: Set<ExerciseRef>,
    onToggle: (ExerciseRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = Theme.shapes.surface
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (lifted) Theme.backgrounds.surfaceRaised else Theme.backgrounds.surface)
            .border(1.dp, if (lifted) Theme.contentColors.action else Theme.backgrounds.separator, shape)
            .padding(start = Theme.spacing.small, end = Theme.spacing.small, top = Theme.spacing.small, bottom = Theme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            block.setType?.takeIf { block.exercises.size > 1 }?.getTitle(context)?.let { type ->
                AppText(
                    modifier = Modifier.padding(start = EditSizes.PickRowStart, top = Theme.spacing.small),
                    text = type.uppercase(),
                    style = Theme.typography.small,
                    color = Theme.contentColors.secondary,
                )
            }
            block.exercises.forEachIndexed { i, exercise ->
                val ref = ExerciseRef(block.id, i)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = EditSizes.PickRow)
                        .clip(Theme.shapes.button)
                        .clickable { onToggle(ref) }
                        .testTag(ExerciseTableTags.editPick(index, i))
                        .padding(horizontal = Theme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
                ) {
                    AppCheck(checked = ref in picked)
                    AppText(text = exercise.exercise?.name.orEmpty(), style = Theme.typography.bodyBold, maxLines = 2)
                }
            }
            AppText(
                modifier = Modifier.padding(start = EditSizes.PickRowStart),
                text = pluralStringResource(R.plurals.workout_log_sets, block.rounds.size, block.rounds.size),
                style = Theme.typography.caption,
                color = Theme.contentColors.secondary,
            )
        }
        Box(modifier = gripModifier.size(EditSizes.Grip), contentAlignment = Alignment.Center) {
            AppIcon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_drag),
                tint = Theme.contentColors.secondary,
                contentDescription = stringResource(R.string.workout_log_move),
            )
        }
    }
}

