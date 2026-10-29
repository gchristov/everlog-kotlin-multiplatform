package com.everlog.android.showcase.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.elements.AppIcon
import com.everlog.android.common.compose.elements.AppIconButton
import com.everlog.android.common.compose.elements.AppMenuItem
import com.everlog.android.common.compose.elements.AppOverflowMenu
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

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
        group(key = "buttons", header = { "Icon buttons: enabled, disabled and outlined" }) {}
        item(key = "buttonsExample") {
            IconButtons(onButtonClick = onButtonClick)
        }
    }
}

@Composable
private fun AppIcons() {
    val icons = listOf(
        DesignR.drawable.ic_back,
        DesignR.drawable.ic_clear_white,
        DesignR.drawable.ic_add,
        DesignR.drawable.ic_remove_white,
        DesignR.drawable.ic_edit,
        DesignR.drawable.ic_delete,
        DesignR.drawable.ic_share,
        DesignR.drawable.ic_timer,
        DesignR.drawable.ic_settings,
        DesignR.drawable.ic_more,
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
        Theme.contentColors.destructiveSoft,
    )
    val star = ImageVector.vectorResource(DesignR.drawable.ic_star_filled)
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
            icon = ImageVector.vectorResource(DesignR.drawable.ic_add),
            tint = Theme.contentColors.action,
            contentDescription = "Add",
        )
        AppIconButton(
            onClick = onButtonClick,
            icon = ImageVector.vectorResource(DesignR.drawable.ic_add),
            tint = Theme.contentColors.action,
            contentDescription = "Add",
            enabled = false,
        )
        AppIconButton(
            onClick = onButtonClick,
            icon = ImageVector.vectorResource(DesignR.drawable.ic_settings),
            contentDescription = "Settings",
            outlined = true,
        )
        AppOverflowMenu(
            items = listOf(AppMenuItem(text = "Details", onClick = onButtonClick)),
            outlined = true,
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
