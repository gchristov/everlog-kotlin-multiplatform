package com.everlog.android.showcase.feature

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.AppCheck
import com.everlog.android.common.compose.elements.AppIcon
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.AppThumbnail
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

@Composable
internal fun DesignSystemLists(onButtonClick: () -> Unit) {
    val checked = remember { mutableStateListOf(1) }
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
                                imageVector = ImageVector.vectorResource(DesignR.drawable.ic_edit),
                                tint = Theme.contentColors.secondary,
                            )
                        },
                        onClick = onButtonClick,
                    )
                    else -> AppListItem(
                        title = "Leading and trailing",
                        leading = {
                            AppIcon(
                                imageVector = ImageVector.vectorResource(DesignR.drawable.ic_timer),
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
                            imageVector = ImageVector.vectorResource(DesignR.drawable.ic_timer),
                            tint = Theme.contentColors.secondary,
                        )
                    },
                    leadingAlignment = if (top) Alignment.Top else Alignment.CenterVertically,
                )
            }
        }
        group(
            key = "thumbnails",
            header = { "Thumbnails and checks" },
            footer = { "Tap a row to check it. Without an image, the thumbnail shows a placeholder icon." },
        ) {
            items(count = 2, key = { it }) { index ->
                AppListItem(
                    title = if (index == 0) "With an image" else "Without an image",
                    subtitle = "AppThumbnail and AppCheck",
                    leading = { AppThumbnail(imageUrl = SampleImageUrl.takeIf { index == 0 }) },
                    trailing = { AppCheck(checked = index in checked) },
                    onClick = { if (index in checked) checked.remove(index) else checked.add(index) },
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

// An exercise illustration from the dev library
private const val SampleImageUrl = "https://storage.googleapis.com/everlog-9bddf.appspot.com/exercises/illustrations/high%20knees1.webp"

@Preview
@Composable
private fun DesignSystemListsPreview() {
    Theme {
        DesignSystemLists(onButtonClick = {})
    }
}
