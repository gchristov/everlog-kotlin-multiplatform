package com.everlog.ui.design.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.theme.Theme
import com.everlog.android.common.design.R as DesignR

/**
 * A compact pill button, e.g. a row of filters to start from. [AppChipStyle.Default] is a card-coloured
 * chip; [AppChipStyle.Action] is outlined in the accent, like [AppSecondaryButton] at chip height,
 * e.g. Clear all at the end of a row of filters.
 *
 * `ui/views/tab/TabsPage` is its XML twin (`view_tab`, `chip_background`), so change both together.
 *
 * @param selected Tints it in the accent, e.g. the current tab in a row of chips that switch pages.
 * @param dropdown Ends it with a down arrow, when it opens a menu.
 */
@Composable
fun AppChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppChipStyle = AppChipStyle.Default,
    selected: Boolean = false,
    dropdown: Boolean = false,
) {
    val (content, border, background) = when {
        selected -> Triple(Theme.contentColors.action, Theme.contentColors.action, Theme.backgrounds.selected)
        style == AppChipStyle.Default -> Triple(Theme.contentColors.primary, Theme.backgrounds.separator, Theme.backgrounds.surface)
        else -> Triple(Theme.contentColors.action, Theme.contentColors.action, Color.Transparent)
    }
    Row(
        modifier = modifier
            .height(ChipSizes.Height)
            .clip(Theme.shapes.button)
            .border(1.dp, border, Theme.shapes.button)
            .background(background)
            .clickable(onClick = onClick)
            .padding(start = Theme.spacing.large, end = if (dropdown) Theme.spacing.medium else Theme.spacing.large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        AppText(text = text, color = content, style = Theme.typography.button, maxLines = 1)
        if (dropdown) {
            AppIcon(
                modifier = Modifier.size(ChipSizes.Icon),
                imageVector = ImageVector.vectorResource(DesignR.drawable.ic_keyboard_down),
                tint = content,
            )
        }
    }
}

enum class AppChipStyle {
    Default,
    Action,
}

private object ChipSizes {
    val Height = 36.dp
    val Icon = 16.dp
}

@Preview
@Composable
private fun AppChipPreview() {
    Theme {
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            AppChip(text = "Info", onClick = {}, selected = true)
            AppChip(text = "Muscle", onClick = {}, dropdown = true)
            AppChip(text = "Clear all", onClick = {}, style = AppChipStyle.Action)
        }
    }
}
