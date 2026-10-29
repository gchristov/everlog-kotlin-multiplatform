package com.everlog.ui.design.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.theme.Theme

// The label above a section, usually an AppSurface card, same as the XML SettingsCategory style
// (e.g. "General" in Settings, "Statistics" on the week screen).
@Composable
fun AppSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    AppText(
        modifier = modifier,
        text = text,
        style = Theme.typography.subtitle,
        color = Theme.contentColors.secondary,
        maxLines = 1,
    )
}

@Preview
@Composable
private fun AppSectionHeaderPreview() {
    Theme {
        AppSectionHeader(text = "General")
    }
}
