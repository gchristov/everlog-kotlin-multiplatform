package com.everlog.android.common.compose.elements

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
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
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

/**
 * A faded example of what an empty screen becomes, e.g. sample sets or statistics, which can't be touched or read
 * out. Goes under an [AppExampleLabel], so it isn't taken for the user's data.
 */
@Composable
fun AppExample(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier) {
        // Faded as one layer, so overlapping parts don't show through each other
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = ExampleAlpha
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .clearAndSetSemantics {},
        ) {
            content()
        }
        Box(modifier = Modifier.matchParentSize().blockingTouches())
    }
}

/**
 * EXAMPLE · NOT YOUR DATA, in an outlined pill, over an [AppExample].
 */
@Composable
fun AppExampleLabel(modifier: Modifier = Modifier) {
    AppText(
        modifier = modifier
            .clip(Theme.shapes.button)
            .border(1.dp, Theme.backgrounds.separator, Theme.shapes.button)
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.extraSmall),
        text = stringResource(DesignR.string.example_label),
        style = Theme.typography.small,
        color = Theme.contentColors.secondary,
    )
}

// Lets no touch through to what's under it
private fun Modifier.blockingTouches(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown().consume()
        do {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
        } while (event.changes.any { it.pressed })
    }
}

private const val ExampleAlpha = 0.35f
