package com.everlog.ui.activities.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.everlog.ui.design.elements.AppHeroHeader
import com.everlog.ui.design.theme.Theme

@Composable
internal fun DesignSystemHeroHeader() {
    ShowcasePage(title = "Hero header") {
        group(key = "text", header = { "Title and body" }) {}
        item(key = "textExample") {
            AppHeroHeader(
                title = "Screen title",
                body = "A line of body text under the title, at the top of a screen's content.",
            )
        }
        group(key = "image", header = { "With an image" }) {}
        item(key = "imageExample") {
            AppHeroHeader(
                // Out of the list's margins, as it would be on a screen
                modifier = Modifier.fullBleed(Theme.spacing.large),
                title = "Push day",
                body = "With an image, here a plain colour",
                image = ColorPainter(Theme.backgrounds.surfaceRaised),
            )
        }
    }
}

// Widens the content by margin on each side, so it reaches the screen edges from inside a padded list
private fun Modifier.fullBleed(margin: Dp) = layout { measurable, constraints ->
    val bleed = margin.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.maxWidth + bleed * 2,
            maxWidth = constraints.maxWidth + bleed * 2,
        )
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-bleed, 0)
    }
}

@Preview
@Composable
private fun DesignSystemHeroHeaderPreview() {
    Theme {
        DesignSystemHeroHeader()
    }
}
