package com.everlog.ui.design.elements.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.elements.AppSectionHeader
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme

/**
 * One group on its own: an optional [AppSectionHeader], its rows drawn as one card, and an optional
 * footer note under them. It looks the same as a group in [AppGroupedList], but doesn't scroll, so
 * it can sit inside other scrolling content.
 */
@Composable
fun AppListGroup(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    showDividers: Boolean = true,
    rows: AppListGroupScope.() -> Unit,
) {
    val rowContents = AppListGroupScope().apply(rows).rows
    Column(modifier = modifier.fillMaxWidth()) {
        header?.let { AppListGroupHeader(text = it) }
        rowContents.forEachIndexed { index, row ->
            AppListGroupRow(
                shape = appListGroupRowShape(index = index, count = rowContents.size),
                showDivider = showDividers && index < rowContents.lastIndex,
                content = row,
            )
        }
        footer?.let { AppListGroupFooter(text = it) }
    }
}

class AppListGroupScope internal constructor() {
    internal val rows = mutableListOf<@Composable () -> Unit>()

    fun row(content: @Composable () -> Unit) {
        rows += content
    }
}

// The pieces of a group, shared with AppGroupedList so both look the same

@Composable
internal fun AppListGroupHeader(text: String) {
    AppSectionHeader(
        modifier = Modifier.padding(bottom = Theme.spacing.large),
        text = text,
    )
}

@Composable
internal fun AppListGroupRow(
    shape: Shape,
    showDivider: Boolean,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Theme.backgrounds.surface)
    ) {
        content()

        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = Theme.backgrounds.separator,
            )
        }
    }
}

// A note under a group's rows, like the footers under Android's settings sections
@Composable
internal fun AppListGroupFooter(text: String) {
    AppText(
        modifier = Modifier.padding(top = Theme.spacing.small),
        text = text,
        style = Theme.typography.caption,
        color = Theme.contentColors.secondary,
    )
}

@Composable
internal fun appListGroupRowShape(index: Int, count: Int): Shape = when {
    count == 1 -> Theme.shapes.groupSingle
    index == 0 -> Theme.shapes.groupStart
    index == count - 1 -> Theme.shapes.groupEnd
    else -> Theme.shapes.groupMiddle
}

@Preview
@Composable
private fun AppListGroupPreview() {
    Theme {
        AppListGroup(
            header = "Reminders",
            footer = "Android will ask to allow notifications.",
        ) {
            row { AppListItem(title = "Time") }
            row { AppListItem(title = "Days") }
        }
    }
}
