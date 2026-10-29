package com.everlog.ui.components.reorder

/**
 * Which way a dragged item moves among its neighbours, by where its middle is: past the middle of the next one it
 * swaps with it (1), past the middle of the previous one with that (-1), and otherwise it stays (0). A missing
 * neighbour (at either end, or not laid out yet) never swaps.
 */
internal fun swapStep(middle: Float, previousMiddle: Float?, nextMiddle: Float?): Int = when {
    nextMiddle != null && middle > nextMiddle -> 1
    previousMiddle != null && middle < previousMiddle -> -1
    else -> 0
}

/**
 * How far to scroll a list this frame while an item spanning [top] to [bottom] is dragged in a view spanning
 * [viewTop] to [viewBottom]: towards an edge the item is within [edge] of, faster the closer it is, up to [maxStep].
 * Negative scrolls up. Near both edges at once (an item taller than the view), up wins.
 */
internal fun autoScrollStep(top: Float, bottom: Float, viewTop: Float, viewBottom: Float, edge: Float, maxStep: Float): Float = when {
    top < viewTop + edge -> -maxStep * ((viewTop + edge - top) / edge).coerceAtMost(1f)
    bottom > viewBottom - edge -> maxStep * ((bottom - (viewBottom - edge)) / edge).coerceAtMost(1f)
    else -> 0f
}
