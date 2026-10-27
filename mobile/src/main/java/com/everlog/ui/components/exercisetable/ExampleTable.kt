package com.everlog.ui.components.exercisetable

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme

/**
 * A faded example of what the table becomes, under an EXAMPLE label, which can't be touched: an empty screen's
 * placeholder.
 */
@Composable
internal fun ExampleTable(blocks: List<WorkoutBlock>, view: TableView, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        AppText(
            modifier = Modifier
                .padding(horizontal = Theme.spacing.large, vertical = Theme.spacing.small)
                .clip(Theme.shapes.button)
                .border(1.dp, Theme.backgrounds.separator, Theme.shapes.button)
                .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.extraSmall),
            text = stringResource(R.string.workout_log_example),
            style = Theme.typography.small,
            color = Theme.contentColors.secondary,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            // Faded as one layer, so overlapping parts don't show through each other
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 0.35f
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .clearAndSetSemantics {},
            ) {
                ExerciseTable(
                    blocks = blocks,
                    view = view,
                    reveal = 0,
                    contentPadding = contentPadding,
                    actions = object : ExerciseTableActions {},
                )
            }
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            })
        }
    }
}
