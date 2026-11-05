package com.everlog.android.common.compose.elements

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

/**
 * A faded example of what an empty screen becomes, e.g. sample sets or statistics, which can't be tapped or read
 * out, though what it's in still scrolls. Goes under an [AppExampleLabel], so it isn't taken for the user's data.
 * Android views inside, e.g. charts, get their touches past Compose: turn theirs off.
 */
@Composable
fun AppExample(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // Faded as one layer, so overlapping parts don't show through each other
    Box(
        modifier = modifier
            .cancellingTaps()
            .graphicsLayer {
                alpha = ExampleAlpha
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .clearAndSetSemantics {},
    ) {
        content()
    }
}

/**
 * EXAMPLE · NOT YOUR DATA, in an outlined pill, over an [AppExample].
 *
 * @param floating Over content that scrolls behind it: blurs it, like a floating [AppChip]. Needs an [AppScreen] or
 * [AppBlurBox] around the content to blur, and has a solid background otherwise.
 */
@Composable
fun AppExampleLabel(modifier: Modifier = Modifier, floating: Boolean = false) {
    AppText(
        modifier = modifier
            .clip(Theme.shapes.button)
            .border(1.dp, Theme.backgrounds.separator, Theme.shapes.button)
            .then(if (floating) Modifier.appBlur() else Modifier)
            .padding(horizontal = Theme.spacing.medium, vertical = Theme.spacing.extraSmall),
        text = stringResource(DesignR.string.example_label),
        style = Theme.typography.small,
        color = Theme.contentColors.secondary,
    )
}

// Takes each lift of a finger before the content sees it, which cancels its taps, and leaves drags to scroll
private fun Modifier.cancellingTaps(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { if (it.changedToUp()) it.consume() }
        }
    }
}

private const val ExampleAlpha = 0.35f
