package com.everlog.ui.design.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
        // Text only, e.g. Skip
        Tertiary,
    }
}

// Full-width actions pinned to the bottom of the screen, as AppScreen's footer. On tablets the
// background still goes edge to edge, and the actions are centred in AppScreen's content width. Like the XML
// footers (RealtimeBlurView), it blurs the content scrolling behind it under a translucent tint.
// Blur needs Android 12+, so older versions get a solid background instead: the tint alone would
// leave the content sharp behind the buttons.
@Composable
fun AppFooter(
    actions: List<AppFooterAction>,
    modifier: Modifier = Modifier,
) {
    val hazeState = LocalAppScreenHazeState.current
    val overlay = Theme.backgrounds.blurOverlay

    Box(
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
                            fallbackTint = HazeTint(Theme.backgrounds.primary),
                        ),
                    )
                } else {
                    Modifier.background(Theme.backgrounds.primary)
                }
            )
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = LocalAppScreenMaxContentWidth.current)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FooterActions(actions)
        }
    }
}

@Composable
private fun FooterActions(actions: List<AppFooterAction>) {
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

            AppFooterAction.Style.Tertiary -> AppTertiaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = action.onClick,
                text = action.text,
                enabled = action.enabled,
            )
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
                AppFooterAction(text = "Tertiary", onClick = {}, style = AppFooterAction.Style.Tertiary),
            )
        )
    }
}
