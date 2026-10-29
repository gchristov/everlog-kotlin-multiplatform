package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme

@Composable
internal fun DesignSystemTypography() {
    ShowcasePage(title = "Typography") {
        group(key = "styles", header = { "Text styles" }) {}
        item(key = "stylesExample") {
            TypographyExamples()
        }
    }
}

@Composable
private fun TypographyExamples() {
    val styles = listOf(
        "title" to Theme.typography.title,
        "heading" to Theme.typography.heading,
        "subtitle" to Theme.typography.subtitle,
        "body" to Theme.typography.body,
        "bodyBold" to Theme.typography.bodyBold,
        "caption" to Theme.typography.caption,
        "small" to Theme.typography.small,
        "button" to Theme.typography.button,
    )
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
        styles.forEach { (name, style) ->
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                AppText(
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline(),
                    text = name,
                    style = style,
                )
                AppText(
                    modifier = Modifier.alignByBaseline(),
                    text = style.description(),
                    style = Theme.typography.small,
                    color = Theme.contentColors.secondary,
                )
            }
        }
    }
}

private fun TextStyle.description(): String {
    val weight = when (fontWeight) {
        FontWeight.Medium -> "Medium"
        FontWeight.Bold -> "Bold"
        else -> "Regular"
    }
    return "${fontSize.value.toInt()}sp · $weight"
}

@Preview
@Composable
private fun DesignSystemTypographyPreview() {
    Theme {
        DesignSystemTypography()
    }
}
