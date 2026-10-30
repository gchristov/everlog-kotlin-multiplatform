package com.everlog.ui.components.exercisetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.theme.Theme
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.ui.components.reorder.ReorderGrip
import com.everlog.ui.components.reorder.rememberReorderState
import com.everlog.ui.components.reorder.reorderGrip
import com.everlog.ui.components.reorder.reorderItem
import com.everlog.ui.components.reorder.reorderViewport
import com.everlog.utils.format.getTitle

private object EditSizes {
    val CardGap = 12.dp
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
    val scroll = rememberScrollState()
    val reorder = rememberReorderState(scroll, blocks.map { it.id }, contentPadding.calculateBottomPadding(), onMove)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ExerciseTableTags.EditList)
            .reorderViewport(reorder)
            .verticalScroll(scroll)
            .padding(horizontal = Theme.spacing.large)
            .padding(top = Theme.spacing.medium, bottom = contentPadding.calculateBottomPadding() + Theme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(EditSizes.CardGap),
    ) {
        blocks.forEachIndexed { index, block ->
            key(block.id) {
                EditCard(
                    modifier = Modifier
                        .testTag(ExerciseTableTags.editCard(index))
                        .reorderItem(reorder, block.id),
                    block = block,
                    lifted = reorder.dragging == block.id,
                    gripModifier = Modifier
                        .testTag(ExerciseTableTags.editGrip(index))
                        .reorderGrip(reorder, block.id),
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
        ReorderGrip(modifier = gripModifier.alignBy { it.measuredHeight / 2 })
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
