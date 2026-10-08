package com.everlog.ui.design.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import com.everlog.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.everlog.ui.design.theme.Theme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * On tablets the content and the footer's actions are capped at [content_width_tablet] and centred,
 * like the login screens, and so is the footer's card. The top bar still goes edge to edge. Proper
 * tablet layouts will come later.
 *
 * @param modifier E.g. `Modifier.nestedScroll(...)` for an [AppBarScrollBehavior].
 * @param footer Pinned to the bottom, usually an [AppFooter].
 * @param content Receives the padding that keeps content clear of the [footer]. Content is
 * drawn behind the footer, which blurs it (see [AppFooter]), so apply the padding inside
 * scrolling containers, e.g. as a LazyColumn's contentPadding.
 */
@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    val hazeState = remember { HazeState() }
    val maxContentWidth = if (booleanResource(R.bool.isTablet)) {
        dimensionResource(R.dimen.content_width_tablet)
    } else {
        Dp.Unspecified
    }

    CompositionLocalProvider(
        LocalAppScreenHazeState provides hazeState,
        LocalAppScreenMaxContentWidth provides maxContentWidth,
    ) {
        Surface(
            modifier = modifier,
            color = Theme.backgrounds.primary,
            contentColor = Theme.contentColors.primary,
        ) {
            val insets = ScaffoldDefaults.contentWindowInsets
            // Like Scaffold, but the content follows the bars in the same frame. Scaffold hands its
            // content the bars' sizes through state, which the content only picks up a frame later,
            // so e.g. a top bar's header leaving showed the content under the old height for a frame.
            SubcomposeLayout { constraints ->
                val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
                val topBarPlaceables = subcompose(AppScreenSlot.TopBar, topBar).map { it.measure(looseConstraints) }
                val footerPlaceables = subcompose(AppScreenSlot.Footer, footer).map { it.measure(looseConstraints) }
                val topBarHeight = topBarPlaceables.maxOfOrNull { it.height } ?: 0
                val footerHeight = footerPlaceables.maxOfOrNull { it.height } ?: 0
                // Without a bar, the content keeps clear of the system bars on that side instead
                val top = if (topBarHeight > 0) topBarHeight else insets.getTop(this)
                val bottom = if (footerHeight > 0) footerHeight else insets.getBottom(this)
                val padding = PaddingValues.Absolute(
                    left = insets.getLeft(this, layoutDirection).toDp(),
                    top = top.toDp(),
                    right = insets.getRight(this, layoutDirection).toDp(),
                    bottom = bottom.toDp(),
                )
                val contentPlaceables = subcompose(AppScreenSlot.Content) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // So children using e.g. imePadding() don't add the system bars again
                            .consumeWindowInsets(padding)
                            .hazeSource(hazeState)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .widthIn(max = maxContentWidth)
                                .fillMaxSize()
                        ) {
                            content(PaddingValues(bottom = padding.calculateBottomPadding()))
                        }
                    }
                }.map {
                    // Under the top bar, and down to the bottom of the screen, behind the footer
                    it.measure(looseConstraints.copy(maxHeight = (constraints.maxHeight - top).coerceAtLeast(0)))
                }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    contentPlaceables.forEach { it.place(0, top) }
                    topBarPlaceables.forEach { it.place(0, 0) }
                    footerPlaceables.forEach { it.place(0, constraints.maxHeight - it.height) }
                }
            }
        }
    }
}

private enum class AppScreenSlot {
    TopBar,
    Footer,
    Content,
}

// Content of the current AppScreen, for bars that blur what's behind them
internal val LocalAppScreenHazeState = staticCompositionLocalOf<HazeState?> { null }

// Widest the current AppScreen's content gets, for bars that line their contents up with it. The
// top bar isn't capped, so anything in it that belongs with the content (e.g. a progress bar under
// the app bar) should use this.
val LocalAppScreenMaxContentWidth = staticCompositionLocalOf { Dp.Unspecified }
