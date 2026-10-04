package com.everlog.ui.design.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.theme.Theme
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

data class AppFooterAction(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val style: Style = Style.Primary,
) {
    enum class Style {
        Primary,
        Secondary,
    }
}

// Full-width actions pinned to the bottom of the screen, as AppScreen's footer. Like the XML
// footers (RealtimeBlurView), it blurs the content scrolling behind it under a translucent tint.
// Blur needs Android 12+, so older versions only get the tint.
@Composable
fun AppFooter(
    actions: List<AppFooterAction>,
    modifier: Modifier = Modifier,
) {
    val hazeState = LocalAppScreenHazeState.current
    val overlay = Theme.backgrounds.blurOverlay

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = Theme.backgrounds.primary,
                            tint = HazeTint(overlay),
                            blurRadius = 20.dp,
                            noiseFactor = 0f,
                        ),
                    )
                } else {
                    Modifier.background(overlay)
                }
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            when (action.style) {
                AppFooterAction.Style.Primary -> AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = action.onClick,
                    text = action.text,
                    enabled = action.enabled,
                )

                AppFooterAction.Style.Secondary -> AppSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = action.onClick,
                    text = action.text,
                    enabled = action.enabled,
                )
            }
        }
    }
}

@Preview
@Composable
private fun AppFooterPreview() {
    Theme {
        AppFooter(
            actions = listOf(
                AppFooterAction(text = "Primary", onClick = {}),
                AppFooterAction(text = "Secondary", onClick = {}, style = AppFooterAction.Style.Secondary),
            )
        )
    }
}
