package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.R
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.theme.Theme

@Composable
internal fun DesignSystemIcons(onButtonClick: () -> Unit) {
    ShowcasePage(title = "Icons") {
        group(key = "icons", header = { "The app's icons, in the default tint" }) {}
        item(key = "iconsExample") {
            AppIcons()
        }
        group(key = "tints", header = { "Content colours" }) {}
        item(key = "tintsExample") {
            Tints()
        }
        group(key = "buttons", header = { "Icon buttons, enabled and disabled" }) {}
        item(key = "buttonsExample") {
            IconButtons(onButtonClick = onButtonClick)
        }
    }
}

@Composable
private fun AppIcons() {
    val icons = listOf(
        R.drawable.ic_back,
        R.drawable.ic_clear_white,
        R.drawable.ic_add,
        R.drawable.ic_remove_white,
        R.drawable.ic_edit,
        R.drawable.ic_delete,
        R.drawable.ic_share,
        R.drawable.ic_timer,
        R.drawable.ic_settings,
        R.drawable.ic_more,
    ).map { ImageVector.vectorResource(it) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        icons.forEach { icon ->
            AppIcon(imageVector = icon)
        }
    }
}

@Composable
private fun Tints() {
    val tints = listOf(
        Theme.contentColors.primary,
        Theme.contentColors.secondary,
        Theme.contentColors.action,
        Theme.contentColors.destructive,
    )
    val star = ImageVector.vectorResource(R.drawable.ic_star_filled)
    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraLarge)) {
        tints.forEach { tint ->
            AppIcon(
                imageVector = star,
                tint = tint,
            )
        }
    }
}

@Composable
private fun IconButtons(onButtonClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
        AppIconButton(
            onClick = onButtonClick,
            icon = ImageVector.vectorResource(R.drawable.ic_add),
            tint = Theme.contentColors.action,
            contentDescription = "Add",
        )
        AppIconButton(
            onClick = onButtonClick,
            icon = ImageVector.vectorResource(R.drawable.ic_add),
            tint = Theme.contentColors.action,
            contentDescription = "Add",
            enabled = false,
        )
    }
}

@Preview
@Composable
private fun DesignSystemIconsPreview() {
    Theme {
        DesignSystemIcons(onButtonClick = {})
    }
}
