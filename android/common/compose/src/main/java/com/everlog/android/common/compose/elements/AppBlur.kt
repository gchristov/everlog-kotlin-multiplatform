package com.everlog.android.common.compose.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.theme.Theme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * [content] with [overlay] floating over it, which blurs the content behind it the way [AppFooter] blurs an
 * [AppScreen]'s: for content outside an AppScreen, e.g. a page in a tab, with e.g. a floating [AppChip] over it.
 */
@Composable
fun AppBlurBox(
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val hazeState = remember { HazeState() }
    Box(modifier = modifier) {
        Box(modifier = Modifier.matchParentSize().hazeSource(hazeState), content = content)
        CompositionLocalProvider(LocalAppScreenHazeState provides hazeState) {
            overlay()
        }
    }
}

// The content behind, blurred under a translucent tint, or a solid background where there's nothing to blur. Blur
// needs Android 12+, so older versions get the solid background too: the tint alone would leave the content sharp.
@Composable
internal fun Modifier.appBlur(): Modifier {
    val hazeState = LocalAppScreenHazeState.current
    return if (hazeState != null) {
        hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = Theme.backgrounds.primary,
                tint = HazeTint(Theme.backgrounds.blurOverlay),
                blurRadius = 20.dp,
                noiseFactor = 0f,
                fallbackTint = HazeTint(Theme.backgrounds.primary),
            ),
        )
    } else {
        background(Theme.backgrounds.primary)
    }
}
