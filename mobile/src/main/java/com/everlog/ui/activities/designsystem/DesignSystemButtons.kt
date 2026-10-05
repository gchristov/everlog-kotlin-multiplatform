package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.AppTertiaryButton
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme

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
        group(key = "fullWidth", header = { "Full width" }) {}
        item(key = "fullWidthExample") {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onButtonClick,
                    text = "Primary",
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
