package com.everlog.ui.design.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import com.everlog.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
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
 * like the login screens. The top bar and the footer's background still go edge to edge. Proper
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
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = footer,
            containerColor = Theme.backgrounds.primary,
            contentColor = Theme.contentColors.primary,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
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
        }
    }
}

// Content of the current AppScreen, for bars that blur what's behind them
internal val LocalAppScreenHazeState = staticCompositionLocalOf<HazeState?> { null }

// Widest the current AppScreen's content gets, for bars that line their contents up with it. The
// top bar isn't capped, so anything in it that belongs with the content (e.g. a progress bar under
// the app bar) should use this.
val LocalAppScreenMaxContentWidth = staticCompositionLocalOf { Dp.Unspecified }
