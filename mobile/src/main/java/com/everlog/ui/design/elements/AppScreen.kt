package com.everlog.ui.design.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
 * @param content Receives the padding that keeps content clear of the [bottomBar]. Content is
 * drawn behind the bottom bar, which blurs it (see [AppFooter]), so apply the padding inside
 * scrolling containers, e.g. as a LazyColumn's contentPadding.
 */
@Composable
fun AppScreen(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    val hazeState = remember { HazeState() }

    CompositionLocalProvider(LocalAppScreenHazeState provides hazeState) {
        Scaffold(
            topBar = topBar,
            bottomBar = bottomBar,
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
                content(PaddingValues(bottom = padding.calculateBottomPadding()))
            }
        }
    }
}

// Content of the current AppScreen, for bars that blur what's behind them
internal val LocalAppScreenHazeState = staticCompositionLocalOf<HazeState?> { null }
