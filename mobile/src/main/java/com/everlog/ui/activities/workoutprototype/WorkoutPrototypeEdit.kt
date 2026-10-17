package com.everlog.ui.activities.workoutprototype

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.everlog.R
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
}

/**
 * Edit workout: the exercises as cards, with the main screen's headers, to drag into a new order
 * by their grip. A superset or circuit moves as one.
 */
@Composable
internal fun EditList(
    state: WorkoutPrototypeState,
    viewModel: WorkoutPrototypeViewModel,
    contentPadding: PaddingValues,
) {
    val blocks by rememberUpdatedState(state.blocks)
    val heights = remember { mutableStateMapOf<Long, Int>() }
    var dragging by remember { mutableStateOf<Long?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val gap = with(LocalDensity.current) { EditSizes.CardGap.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Theme.spacing.large)
            .padding(top = Theme.spacing.extraSmall, bottom = contentPadding.calculateBottomPadding() + Theme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(EditSizes.CardGap),
    ) {
        state.blocks.forEach { block ->
            key(block.id) {
                val lifted = dragging == block.id
                EditCard(
                    modifier = Modifier
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
                    gripModifier = Modifier.pointerInput(block.id) {
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
                                val index = blocks.indexOfFirst { it.id == block.id }
                                if (offset > 0 && index < blocks.lastIndex) {
                                    val step = (heights[blocks[index + 1].id] ?: 0) + gap
                                    if (offset > step / 2) {
                                        viewModel.onMoveBlock(index, index + 1)
                                        offset -= step
                                    }
                                } else if (offset < 0 && index > 0) {
                                    val step = (heights[blocks[index - 1].id] ?: 0) + gap
                                    if (-offset > step / 2) {
                                        viewModel.onMoveBlock(index, index - 1)
                                        offset += step
                                    }
                                }
                            },
                        )
                    },
                    onDelete = { viewModel.onDeleteBlock(block.id) },
                )
            }
        }
        // A note under the last card, like a list group's footer: the cards' 8dp gap above it.
        // Add exercise is in the screen's footer.
        AppText(
            modifier = Modifier.fillMaxWidth(),
            text = "Drag the grip to move. Tap the bin to delete.",
            style = Theme.typography.caption,
            color = Theme.contentColors.secondary,
        )
        Spacer(modifier = Modifier.height(Theme.spacing.large))
    }
}

@Composable
private fun EditCard(
    block: PrototypeBlock,
    lifted: Boolean,
    gripModifier: Modifier,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = Theme.shapes.surface
    AppListItem(
        modifier = modifier
            .clip(shape)
            .background(if (lifted) Theme.backgrounds.surfaceRaised else Theme.backgrounds.surface)
            .border(1.dp, if (lifted) Theme.contentColors.action else Theme.backgrounds.separator, shape),
        header = when (block.kind) {
            BlockKind.Single -> null
            BlockKind.Superset -> "Superset"
            BlockKind.Circuit -> "Circuit"
        },
        // One exercise per line, like the main screen's headers
        title = block.exercises.joinToString("\n") { it.name },
        subtitle = if (block.rounds.size == 1) "1 set" else "${block.rounds.size} sets",
        leading = {
            Box(modifier = gripModifier.size(EditSizes.Grip), contentAlignment = Alignment.Center) {
                AppIcon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_drag),
                    tint = Theme.contentColors.secondary,
                    contentDescription = "Move",
                )
            }
        },
        // Delete straight away (it asks to confirm), in the circle the menus use
        trailing = {
            Box(modifier = Modifier.outlinedCircle()) {
                AppIconButton(
                    onClick = onDelete,
                    icon = ImageVector.vectorResource(R.drawable.ic_delete),
                    tint = Theme.contentColors.destructive,
                    contentDescription = "Delete",
                )
            }
        },
    )
}
