package com.everlog.android.common.compose.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.theme.Theme

/**
 * Whether a setting is on, at the end of a row: the track filled in the accent when on.
 * It only shows the state; the row handles the tap, so the whole row toggles it.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Theme.contentColors.onAction,
            checkedTrackColor = Theme.contentColors.action,
            checkedBorderColor = Theme.contentColors.action,
            uncheckedThumbColor = Theme.contentColors.secondary,
            uncheckedTrackColor = Theme.backgrounds.inset,
            uncheckedBorderColor = Theme.backgrounds.separator,
            disabledCheckedThumbColor = Theme.contentColors.onAction.copy(alpha = DisabledAlpha),
            disabledCheckedTrackColor = Theme.contentColors.action.copy(alpha = DisabledAlpha),
            disabledCheckedBorderColor = Theme.contentColors.action.copy(alpha = DisabledAlpha),
            disabledUncheckedThumbColor = Theme.contentColors.secondary.copy(alpha = DisabledAlpha),
            disabledUncheckedTrackColor = Theme.backgrounds.inset.copy(alpha = DisabledAlpha),
            disabledUncheckedBorderColor = Theme.backgrounds.separator.copy(alpha = DisabledAlpha),
        ),
    )
}

@Preview
@Composable
private fun AppSwitchPreview() {
    Theme {
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            AppSwitch(checked = false)
            AppSwitch(checked = true)
            AppSwitch(checked = false, enabled = false)
            AppSwitch(checked = true, enabled = false)
        }
    }
}
