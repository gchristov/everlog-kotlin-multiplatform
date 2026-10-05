package com.everlog.ui.design.elements.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            .padding(Theme.spacing.large),
        contentAlignment = Alignment.CenterStart,
    ) {
        content()
    }
}

// A row like the ones in Settings (SettingsTitle and SettingsSubtitle): a title with an optional
// header above it and subtitle below it, between optional leading and trailing content, laid out like
// the newsfeed project's feed rows.
@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    header: String? = null,
    subtitle: String? = null,
    titleColor: Color = Theme.contentColors.primary,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    AppListRow(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large),
        ) {
            leading?.invoke()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
            ) {
                header?.let {
                    AppText(
                        text = it,
                        style = Theme.typography.caption,
                        color = Theme.contentColors.secondary,
                    )
                }
                AppText(
                    text = title,
                    style = Theme.typography.body,
                    color = titleColor,
                )
                subtitle?.let {
                    AppText(
                        text = it,
                        style = Theme.typography.caption,
                        color = Theme.contentColors.secondary,
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

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
