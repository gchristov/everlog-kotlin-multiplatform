package com.everlog.ui.components.reorder

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

/**
 * A list's drag to reorder: the dragged item follows the finger, swaps places with a neighbour once its middle passes
 * the neighbour's, and scrolls the list while it's near the list's top or bottom. Positions are in the window, so the
 * item stays under the finger however the list scrolls or relays out (e.g. its items collapsing while it's dragged).
 * Created by [rememberReorderState].
 */
@Stable
internal class ReorderState(
    private val scroll: ScrollState,
    private val edge: Float,
    private val maxScrollStep: Float,
) {
    var dragging by mutableStateOf<Long?>(null)
        private set

    // The item just dropped, which the list brings into view
    internal var dropped by mutableStateOf<Long?>(null)

    // Where the dragged item was when the drag started, and how far the finger has gone since
    private var dragStart by mutableFloatStateOf(0f)
    private var dragDistance by mutableFloatStateOf(0f)
    private val tops = mutableStateMapOf<Long, Float>()
    private val heights = mutableStateMapOf<Long, Int>()
    internal var viewTop by mutableFloatStateOf(0f)
    internal var viewBottom by mutableFloatStateOf(0f)
    internal var bottomInset = 0f
    internal var keys: List<Long> = emptyList()
    internal var onMove: (from: Int, to: Int) -> Unit = { _, _ -> }

    internal fun laidOut(key: Long, top: Float, height: Int) {
        tops[key] = top
        heights[key] = height
    }

    internal fun start(key: Long) {
        dragStart = tops[key] ?: 0f
        dragDistance = 0f
        dropped = null
        dragging = key
    }

    internal fun drag(distance: Float) {
        dragDistance += distance
        swapPastNeighbours()
    }

    internal fun end() {
        dropped = dragging
        dragging = null
    }

    // How far the dragged item is drawn from where it's laid out
    internal fun translation(key: Long): Float = if (dragging == key) dragStart + dragDistance - (tops[key] ?: 0f) else 0f

    private fun swapPastNeighbours() {
        val key = dragging ?: return
        val at = keys.indexOf(key)
        if (at < 0) return
        val middle = dragStart + dragDistance + (heights[key] ?: 0) / 2f
        val to = at + swapStep(middle, middleOf(keys.getOrNull(at - 1)), middleOf(keys.getOrNull(at + 1)))
        if (to == at) return
        // Moved here too, so another event before the list recomposes doesn't move it back
        keys = keys.toMutableList().apply { add(to, removeAt(at)) }
        onMove(at, to)
    }

    private fun middleOf(key: Long?): Float? {
        val top = tops[key ?: return null] ?: return null
        return top + (heights[key] ?: 0) / 2f
    }

    // Runs while an item is dragged
    internal suspend fun autoScroll() {
        while (true) {
            val key = dragging ?: return
            val top = dragStart + dragDistance
            val step = autoScrollStep(top, top + (heights[key] ?: 0), viewTop, viewBottom - bottomInset, edge, maxScrollStep)
            if (step != 0f && scroll.scrollBy(step) != 0f) swapPastNeighbours()
            withFrameNanos { }
        }
    }
}
