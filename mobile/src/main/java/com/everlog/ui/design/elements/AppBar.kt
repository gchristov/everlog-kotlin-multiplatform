package com.everlog.ui.design.elements

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.ui.design.theme.Theme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Same as the XML toolbar: black background, 20sp medium title.
 *
 * @param header A larger header under the bar's row, usually an [AppBarHeader]. It's part of the
 * app bar rather than the content, so it sits right under the row on every screen.
 * @param scrollBehavior Makes the [header] scroll away with the content, as if it were the top of
 * the page. The row (back, title, actions) stays. Without it the header stays put. With a [title]
 * as well, the bar collapses: the title only fades into the row as the header scrolls away, so the
 * header's large title becomes the screen's title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    title: String? = null,
    showBack: Boolean = false,
    contentColor: Color = Theme.contentColors.primary,
    actions: @Composable (RowScope.() -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
    scrollBehavior: AppBarScrollBehavior? = null,
) {
    Column(modifier = Modifier.background(Theme.backgrounds.primary)) {
        AppBarRow(
            title = title,
            showBack = showBack,
            contentColor = contentColor,
            actions = actions,
            // Collapsing: the title shows once the header has (mostly) gone
            titleProgress = if (header != null && scrollBehavior != null) {
                { collapsedTitleProgress(scrollBehavior.collapsedFraction) }
            } else {
                null
            },
        )
        header?.let {
            ScrollingHeader(scrollBehavior = scrollBehavior, content = it)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBarRow(
    title: String?,
    showBack: Boolean,
    contentColor: Color,
    actions: @Composable (RowScope.() -> Unit)?,
    // From 0 (hidden) to 1 (showing). Read while drawing, so scrolling doesn't recompose the bar.
    titleProgress: (() -> Float)?,
) {
    val titleOffset = with(LocalDensity.current) { CollapsedTitleOffset.toPx() }
    // A collapsing title is hidden from TalkBack until it starts to show, so the header's title isn't
    // read twice. Only crossing that point recomposes, not every frame of the scroll.
    val currentTitleProgress by rememberUpdatedState(titleProgress)
    val titleShown by remember { derivedStateOf { (currentTitleProgress?.invoke() ?: 1f) > 0f } }
    // Goes through the back dispatcher rather than finishing the activity, so screens can still
    // intercept back (e.g. to confirm discarding changes)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    TopAppBar(
        title = {
            title?.let { title ->
                AppText(
                    modifier = if (titleProgress != null) {
                        Modifier
                            .graphicsLayer {
                                val progress = titleProgress()
                                alpha = progress
                                // Drifts up into place as it fades in
                                translationY = (1f - progress) * titleOffset
                            }
                            .then(if (titleShown) Modifier else Modifier.clearAndSetSemantics {})
                    } else {
                        Modifier
                    },
                    text = title,
                    color = contentColor,
                    style = Theme.typography.heading,
                    maxLines = 1,
                )
            }
        },
        navigationIcon = {
            if (showBack) {
                AppIconButton(
                    icon = ImageVector.vectorResource(R.drawable.ic_back),
                    tint = contentColor,
                    contentDescription = stringResource(R.string.app_bar_back),
                    onClick = { backDispatcher?.onBackPressed() }
                )
            }
        },
        actions = { actions?.invoke(this) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Theme.backgrounds.primary,
            scrolledContainerColor = Theme.backgrounds.primary,
        ),
    )
}

// A collapsing bar's title fades in over the last part of the header scrolling away, roughly as
// the header's own title goes under the bar
private fun collapsedTitleProgress(collapsedFraction: Float): Float =
    ((collapsedFraction - CollapsedTitleStart) / (1f - CollapsedTitleStart)).coerceIn(0f, 1f)

private const val CollapsedTitleStart = 0.5f
private val CollapsedTitleOffset = 8.dp

/**
 * The standard look for an [AppBar]'s header: a large title, with an optional [eyebrow] above it in
 * the accent colour (e.g. "Welcome to Everlog") and a line of [body] text under it. Padded to the
 * screen margin, and as wide as AppScreen's content on tablets.
 */
@Composable
fun AppBarHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    body: String? = null,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = LocalAppScreenMaxContentWidth.current)
                .fillMaxWidth()
                .padding(Theme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        ) {
            eyebrow?.let {
                AppText(
                    text = it,
                    style = Theme.typography.caption,
                    color = Theme.contentColors.action,
                )
            }
            AppText(
                text = title,
                style = Theme.typography.title,
            )
            body?.let {
                AppText(
                    text = it,
                    style = Theme.typography.body,
                    color = Theme.contentColors.secondary,
                )
            }
        }
    }
}

/**
 * Scrolls an [AppBar]'s header away with the content, at the same speed, so it reads as the top of
 * the page rather than a bar. Scrolling up, the header goes first, then the content. Scrolling
 * down, the content goes first and the header comes back once the content is at the top. The header
 * doesn't fold into the bar's title: it just leaves. Give the [AppBar] a title too for it to
 * collapse, with that title fading into the bar as the header goes.
 *
 * Pass the same behaviour to the [AppBar] and to the screen, as
 * `AppScreen(modifier = Modifier.nestedScroll(behavior.nestedScrollConnection))`.
 */
@Stable
class AppBarScrollBehavior internal constructor(
    private val canScroll: () -> Boolean,
    initialHeaderOffset: Float = 0f,
) {
    // How far the header has scrolled away, from 0 (all of it showing) to -headerHeight
    internal var headerOffset by mutableFloatStateOf(initialHeaderOffset)
        private set

    // Set when the header is measured. Not state, as nothing draws from it.
    internal var headerHeight = 0f

    // How much of the header has scrolled away, from 0 (none) to 1 (all of it). Reads the offset
    // first, even before the header is measured, so whatever draws from this redraws as it scrolls.
    internal val collapsedFraction: Float
        get() {
            val offset = headerOffset
            return if (headerHeight > 0f) (-offset / headerHeight).coerceIn(0f, 1f) else 0f
        }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Up: the header goes first, but only while the page is taller than the screen, so a
            // page that fits doesn't scroll at all
            if (available.y >= 0f || !canScroll()) return Offset.Zero
            return Offset(0f, moveHeader(available.y))
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            // Down: whatever the content has left over once it's at the top brings the header back
            if (available.y <= 0f) return Offset.Zero
            return Offset(0f, moveHeader(available.y))
        }
    }

    // Dragging the header itself. While any of the header shows, the content is at the top, so this
    // is the same as scrolling the page.
    internal fun dragHeader(delta: Float): Float {
        if (delta < 0f && !canScroll()) return 0f
        return moveHeader(delta)
    }

    private fun moveHeader(delta: Float): Float {
        val previous = headerOffset
        headerOffset = (headerOffset + delta).coerceIn(-headerHeight, 0f)
        return headerOffset - previous
    }
}

/**
 * @param canScroll Whether the content can still scroll further, e.g.
 * `{ scrollState.canScrollForward }`. The header only scrolls away while it can, as the content
 * of a page that fits on the screen shouldn't move.
 */
@Composable
fun rememberAppBarScrollBehavior(canScroll: () -> Boolean = { true }): AppBarScrollBehavior {
    val currentCanScroll by rememberUpdatedState(canScroll)
    // Saved with the content's scroll position, so the two stay in step after e.g. rotating
    return rememberSaveable(
        saver = Saver(
            save = { it.headerOffset },
            restore = { AppBarScrollBehavior(canScroll = { currentCanScroll() }, initialHeaderOffset = it) },
        ),
    ) {
        AppBarScrollBehavior(canScroll = { currentCanScroll() })
    }
}

// Lays the header out at its full height, then moves it up by however far it has scrolled away and
// gives up that much space, so the content follows it
@Composable
private fun ScrollingHeader(
    scrollBehavior: AppBarScrollBehavior?,
    content: @Composable () -> Unit,
) {
    val decay = rememberSplineBasedDecay<Float>()
    Layout(
        modifier = Modifier
            .clipToBounds()
            .then(
                if (scrollBehavior == null) {
                    Modifier
                } else {
                    // The header isn't part of the scrolling content, so dragging and flinging it
                    // moves it directly
                    Modifier.draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> scrollBehavior.dragHeader(delta) },
                        onDragStopped = { velocity ->
                            var previous = 0f
                            AnimationState(initialValue = 0f, initialVelocity = velocity).animateDecay(decay) {
                                val delta = value - previous
                                previous = value
                                // Stop once the header can't go any further
                                if (abs(scrollBehavior.dragHeader(delta)) < abs(delta) - 0.5f) cancelAnimation()
                            }
                        },
                    )
                }
            ),
        content = { Box { content() } },
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val height = placeable.height
        scrollBehavior?.headerHeight = height.toFloat()
        // Read here, not in composition, so scrolling only moves the header
        val offset = (scrollBehavior?.headerOffset ?: 0f).roundToInt().coerceIn(-height, 0)
        layout(placeable.width, height + offset) {
            placeable.place(0, offset)
        }
    }
}
