package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme

@Composable
internal fun DesignSystemLists(onButtonClick: () -> Unit) {
    ShowcasePage(title = "Lists") {
        group(
            key = "items",
            header = { "List items" },
            footer = { "A footer note under a group's rows." },
        ) {
            items(count = 5, key = { it }) { index ->
                when (index) {
                    0 -> AppListItem(title = "Title only")
                    1 -> AppListItem(
                        title = "Title and subtitle",
                        subtitle = "Like the rows in Settings",
                    )
                    2 -> AppListItem(
                        title = "Tappable row",
                        subtitle = "Counts as a button tap",
                        onClick = onButtonClick,
                    )
                    3 -> AppListItem(
                        header = "Header",
                        title = "Title in the action colour",
                        titleColor = Theme.contentColors.action,
                        trailing = {
                            AppIcon(
                                modifier = Modifier.size(20.dp),
                                imageVector = ImageVector.vectorResource(R.drawable.ic_edit),
                                tint = Theme.contentColors.secondary,
                            )
                        },
                        onClick = onButtonClick,
                    )
                    else -> AppListItem(
                        title = "Leading and trailing",
                        leading = {
                            AppIcon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_timer),
                                tint = Theme.contentColors.secondary,
                            )
                        },
                        trailing = {
                            AppText(text = "18:00", style = Theme.typography.heading)
                        },
                    )
                }
            }
        }
        group(key = "leadingAlignment", header = { "Leading content alignment" }) {
            items(count = 2, key = { it }) { index ->
                val top = index == 1
                AppListItem(
                    title = if (top) "Aligned to the top" else "Centred",
                    subtitle = "A subtitle long enough to run to several lines, to show where the leading icon sits against the text.",
                    leading = {
                        AppIcon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_timer),
                            tint = Theme.contentColors.secondary,
                        )
                    },
                    leadingAlignment = if (top) Alignment.Top else Alignment.CenterVertically,
                )
            }
        }
        group(key = "single", header = { "A group with one row" }) {
            items(count = 1, key = { it }) {
                AppListItem(
                    title = "Single row",
                    subtitle = "Rounded on all corners",
                )
            }
        }
    }
}

@Preview
@Composable
private fun DesignSystemListsPreview() {
    Theme {
        DesignSystemLists(onButtonClick = {})
    }
}
