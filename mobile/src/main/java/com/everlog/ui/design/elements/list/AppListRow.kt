package com.everlog.ui.design.elements.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme

// A row's padding, for any content, at least Material's one-line list item height with the content
// centred vertically. Rows with a title and subtitle use AppListItem.
@Composable
fun AppListRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListMinHeight)
            .padding(ListSpacing),
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}

// A row with a title and an optional subtitle, like the rows in Settings (SettingsTitle and
// SettingsSubtitle). Leading and trailing content will come once their design is decided.
@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
) {
    AppListRow(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppText(
                text = title,
                style = Theme.typography.body,
            )
            subtitle?.let {
                AppText(
                    text = it,
                    style = Theme.typography.caption,
                    color = Theme.contentColors.secondary,
                )
            }
        }
    }
}

private val ListSpacing = 16.dp
private val ListMinHeight = 56.dp

@Preview
@Composable
private fun AppGroupedListPreview() {
    Theme {
        AppGroupedList {
            group(
                key = "general",
                header = { "General" },
            ) {
                items(count = 2, key = { it }) { index ->
                    AppListItem(
                        title = "Title $index",
                        subtitle = "Subtitle".takeIf { index == 1 },
                    )
                }
            }
        }
    }
}
