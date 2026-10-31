package com.everlog.android.common.compose.components.reorder

import com.everlog.android.common.design.R as DesignR
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.everlog.android.common.compose.elements.AppIcon
import com.everlog.android.common.compose.theme.Theme
import kotlinx.coroutines.android.awaitFrame

private object ReorderSizes {
    // How near the list's edge a dragged item starts it scrolling, and its fastest scroll per frame
    val ScrollEdge = 72.dp
    val MaxScrollStep = 16.dp
    const val LiftScale = 1.02f
}

/**
 * Drag to reorder for a scrolling column whose items are keyed by [keys]: give the column [reorderViewport] (before
 * its scroll), each item [reorderItem] and each item's grip [reorderGrip].
 *
 * @param bottomInset What floats over the column's bottom, e.g. a footer, which the list scrolls before reaching
 * @param onMove Moves the item at one index to the other, one place at a time
 */
@Composable
fun rememberReorderState(
    scroll: ScrollState,
    keys: List<Long>,
    bottomInset: Dp,
    onMove: (from: Int, to: Int) -> Unit,
): ReorderState {
    val density = LocalDensity.current
    val state = remember(scroll, density) {
        with(density) { ReorderState(scroll, ReorderSizes.ScrollEdge.toPx(), ReorderSizes.MaxScrollStep.toPx()) }
    }
    val inset = with(density) { bottomInset.toPx() }
    SideEffect {
        state.keys = keys
        state.onMove = onMove
        state.bottomInset = inset
    }
    LaunchedEffect(state.dragging) { state.autoScroll() }
    return state
}

fun Modifier.reorderViewport(state: ReorderState): Modifier = onGloballyPositioned {
    val bounds = it.boundsInWindow()
    state.viewTop = bounds.top
    state.viewBottom = bounds.bottom
}

/**
 * Follows the finger while dragged, raised over the other items, and comes into view once dropped.
 */
@Composable
fun Modifier.reorderItem(state: ReorderState, key: Long): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val dropped = state.dropped == key
    LaunchedEffect(dropped) {
        if (!dropped) return@LaunchedEffect
        // After the list has laid out again without the drag
        awaitFrame()
        requester.bringIntoView()
        if (state.dropped == key) state.dropped = null
    }
    val lifted = state.dragging == key
    return this
        // Outside the lift, so it's where the item is laid out, not where it's drawn
        .onGloballyPositioned { state.laidOut(key, it.positionInWindow().y, it.size.height) }
        .bringIntoViewRequester(requester)
        .zIndex(if (lifted) 1f else 0f)
        .graphicsLayer {
            translationY = state.translation(key)
            val scale = if (state.dragging == key) ReorderSizes.LiftScale else 1f
            scaleX = scale
            scaleY = scale
        }
}

/**
 * Starts dragging the item [key] when touched and moved.
 *
 * @param onStart Before the drag starts, e.g. to close a keypad
 */
fun Modifier.reorderGrip(state: ReorderState, key: Long, onStart: () -> Unit = {}): Modifier = pointerInput(state, key) {
    detectDragGestures(
        onDragStart = {
            onStart()
            state.start(key)
        },
        onDragEnd = state::end,
        onDragCancel = state::end,
        onDrag = { change, amount ->
            change.consume()
            state.drag(amount.y)
        },
    )
}

/**
 * The grip to drag an item by: the drag icon in an outlined circle, an item's leading size.
 */
@Composable
fun ReorderGrip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Theme.sizes.leading)
            .border(1.dp, Theme.backgrounds.separator, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            imageVector = ImageVector.vectorResource(DesignR.drawable.ic_drag),
            tint = Theme.contentColors.secondary,
            contentDescription = stringResource(DesignR.string.workout_log_move),
        )
    }
}
