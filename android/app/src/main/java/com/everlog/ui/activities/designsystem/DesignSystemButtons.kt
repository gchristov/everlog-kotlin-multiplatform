package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppIconButton
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppTertiaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.AppThumbnail
import com.everlog.ui.design.theme.Theme
import com.everlog.android.common.design.R as DesignR

@Composable
private fun SizeExample(name: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
        AppText(text = name, style = Theme.typography.caption, color = Theme.contentColors.secondary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            content()
        }
    }
}

// Taps on any showcase page add to the count shown here
@Composable
internal fun DesignSystemButtons(
    buttonClicks: Int,
    onButtonClick: () -> Unit,
) {
    ShowcasePage(title = "Buttons") {
        group(key = "buttons", header = { "Primary, secondary and tertiary" }) {}
        item(key = "buttonsExample") {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                AppText(
                    text = if (buttonClicks == 1) "Tapped 1 time" else "Tapped $buttonClicks times",
                    style = Theme.typography.caption,
                    color = Theme.contentColors.secondary,
                )
                ButtonRow(
                    enabled = true,
                    onButtonClick = onButtonClick,
                )
                ButtonRow(
                    enabled = false,
                    onButtonClick = onButtonClick,
                )
            }
        }
        group(key = "sizes", header = { "Sizes: what sits side by side lines up" }) {}
        item(key = "sizesExample") {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                SizeExample("button: an icon button beside a button") {
                    AppIconButton(
                        modifier = Modifier.size(Theme.sizes.button),
                        onClick = onButtonClick,
                        icon = ImageVector.vectorResource(R.drawable.ic_settings),
                        contentDescription = "Options",
                        outlined = true,
                    )
                    AppSecondaryButton(onClick = onButtonClick, text = "Finish")
                }
                SizeExample("leading: beside a title, e.g. an image and an outlined icon button") {
                    AppThumbnail(imageUrl = null, size = Theme.sizes.leading)
                    AppIconButton(
                        onClick = onButtonClick,
                        icon = ImageVector.vectorResource(DesignR.drawable.ic_more),
                        contentDescription = "More",
                        outlined = true,
                    )
                }
                SizeExample("rowControl: inside a row, e.g. a value box") {
                    Box(
                        modifier = Modifier
                            .size(Theme.sizes.rowControl * 2, Theme.sizes.rowControl)
                            .background(Theme.backgrounds.inset, Theme.shapes.button)
                            .border(1.dp, Theme.backgrounds.separator, Theme.shapes.button),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppText(text = "10")
                    }
                }
            }
        }
        group(key = "fullWidth", header = { "Full width" }) {}
        item(key = "fullWidthExample") {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onButtonClick,
                    text = "Primary",
                )
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onButtonClick,
                    text = "With an icon",
                    icon = ImageVector.vectorResource(DesignR.drawable.ic_check),
                )
                AppSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onButtonClick,
                    text = "Secondary",
                )
                AppTertiaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onButtonClick,
                    text = "Tertiary",
                )
            }
        }
    }
}

@Composable
internal fun ButtonRow(
    enabled: Boolean,
    onButtonClick: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        AppButton(
            onClick = onButtonClick,
            text = if (enabled) "Primary" else "Disabled",
            enabled = enabled,
        )
        AppSecondaryButton(
            onClick = onButtonClick,
            text = if (enabled) "Secondary" else "Disabled",
            enabled = enabled,
        )
        AppTertiaryButton(
            onClick = onButtonClick,
            text = if (enabled) "Tertiary" else "Disabled",
            enabled = enabled,
        )
    }
}

@Preview
@Composable
private fun DesignSystemButtonsPreview() {
    Theme {
        DesignSystemButtons(buttonClicks = 0, onButtonClick = {})
    }
}
