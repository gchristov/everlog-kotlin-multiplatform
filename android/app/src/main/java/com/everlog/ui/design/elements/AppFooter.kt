package com.everlog.ui.design.elements

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
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

// Actions pinned to the bottom of the screen, as AppScreen's footer, in a floating card: inset by the
// screen margin so it lines up with the content (AppScreen's content width on tablets), with the
// cards' corners, so they match a card scrolling behind it, and a hairline border. Like the XML
// footers (RealtimeBlurView), it blurs the content scrolling behind it under a translucent tint.
// Blur needs Android 12+, so older versions get a solid background instead: the tint alone would
// leave the content sharp behind the buttons. An optional [header] sits above the actions, e.g. a
// note on what they do.
//
// The screen decides when the footer's content is new: when [transitionKey] changes (e.g. a step of
// a flow), the new header and actions crossfade in place and the card's height follows. Otherwise
// they update in place, so e.g. a count in a button's text ("Add 2 exercises") just changes. Without
// a key it never crossfades. A single action keeps the lower slot, so the thumb target doesn't move.
@Composable
fun AppFooter(
    actions: List<AppFooterAction>,
    modifier: Modifier = Modifier,
    header: String? = null,
    transitionKey: Any? = null,
) {
    val shape = Theme.shapes.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = Theme.spacing.large, end = Theme.spacing.large, bottom = Theme.spacing.small),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedContent(
            targetState = FooterContent(header, actions, transitionKey),
            modifier = Modifier
                .widthIn(max = LocalAppScreenMaxContentWidth.current)
                .fillMaxWidth()
                .clip(shape)
                .footerBlur()
                .border(1.dp, Theme.backgrounds.separator, shape)
                .padding(Theme.spacing.large),
            contentKey = { it.transitionKey },
            contentAlignment = Alignment.BottomCenter,
            transitionSpec = {
                (fadeIn(tween(FooterMotion.Fade)) togetherWith fadeOut(tween(FooterMotion.Fade)))
                    .using(SizeTransform(clip = false) { _, _ -> tween(FooterMotion.Resize) })
            },
            label = "footer",
        ) { content ->
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                content.header?.let { FooterHeader(text = it) }
                FooterActions(content.actions)
            }
        }
    }
}

private data class FooterContent(val header: String?, val actions: List<AppFooterAction>, val transitionKey: Any?)

private object FooterMotion {
    const val Fade = 150
    const val Resize = 250
}

// A note on the actions, in a card on the screen's background so it stands out from the blur behind. The hairline
// keeps it visible when there's nothing behind, as the blur is then the same colour.
@Composable
private fun FooterHeader(text: String) {
    AppText(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Theme.shapes.surface)
            .background(Theme.backgrounds.primary)
            .border(1.dp, Theme.backgrounds.separator, Theme.shapes.surface)
            .padding(Theme.spacing.large),
        text = text,
        style = Theme.typography.caption,
        color = Theme.contentColors.secondary,
    )
}

// The content behind, blurred under the tint, or a solid background where there's no blur
@Composable
private fun Modifier.footerBlur(): Modifier {
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
            header = "A note on the actions",
            actions = listOf(
                AppFooterAction(text = "Primary", onClick = {}),
                AppFooterAction(text = "Secondary", onClick = {}, style = AppFooterAction.Style.Secondary),
                AppFooterAction(text = "Tertiary", onClick = {}, style = AppFooterAction.Style.Tertiary),
            )
        )
    }
}
