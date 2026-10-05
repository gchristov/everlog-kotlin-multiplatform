package com.everlog.ui.activities.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme

// The footer pinned to the bottom, with rows to scroll behind it and see the blur
@Composable
internal fun DesignSystemFooter(onButtonClick: () -> Unit) {
    ShowcasePage(
        title = "Footer",
        footer = {
            AppFooter(
                actions = listOf(
                    AppFooterAction(
                        text = "Primary action",
                        onClick = onButtonClick,
                    ),
                    AppFooterAction(
                        text = "Secondary action",
                        onClick = onButtonClick,
                        style = AppFooterAction.Style.Secondary,
                    ),
                    AppFooterAction(
                        text = "Tertiary action",
                        onClick = onButtonClick,
                        style = AppFooterAction.Style.Tertiary,
                    ),
                )
            )
        },
    ) {
        group(
            key = "rows",
            header = { "Scroll behind the footer" },
            footer = { "The footer blurs what's behind it on Android 12 and later, and has a solid background before that." },
        ) {
            items(count = 20, key = { it }) { index ->
                AppListItem(
                    title = "Row ${index + 1}",
                    subtitle = "Some content to scroll behind the footer",
                )
            }
        }
    }
}

@Preview
@Composable
private fun DesignSystemFooterPreview() {
    Theme {
        DesignSystemFooter(onButtonClick = {})
    }
}
