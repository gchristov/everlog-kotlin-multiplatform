package com.everlog.android.common.compose.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import java.util.Locale
import org.threeten.bp.LocalDate
import org.threeten.bp.format.TextStyle

/**
 * One day on a timeline, as the history lists show it: its date, a dot on a line joining it to the days above and
 * below, then [content], e.g. a list group of the day's workouts. Items go one after another in a list, a group's
 * spacing apart.
 *
 * @param first The top of the timeline, with no line above its dot
 * @param last The bottom, with no line below
 */
@Composable
fun AppTimelineItem(
    date: LocalDate,
    first: Boolean,
    last: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(
            modifier = Modifier.widthIn(min = TimelineSizes.DateWidth).padding(top = TimelineSizes.DateTop, end = Theme.spacing.large),
            horizontalAlignment = Alignment.End,
        ) {
            AppText(text = date.dayOfMonth.toString(), style = Theme.typography.bodyBold, color = Theme.contentColors.secondary)
            AppText(
                text = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                style = Theme.typography.small,
                color = Theme.contentColors.secondary,
            )
            AppText(text = date.year.toString(), style = Theme.typography.small, color = Theme.contentColors.secondary)
        }
        Column(
            modifier = Modifier.fillMaxHeight().padding(end = Theme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TimelineLine(visible = !first, modifier = Modifier.height(TimelineSizes.LineAboveDot))
            Box(
                modifier = Modifier.padding(vertical = Theme.spacing.extraSmall).size(TimelineSizes.Dot)
                    .background(Theme.backgrounds.separator, CircleShape),
            )
            TimelineLine(visible = !last, modifier = Modifier.weight(1f))
        }
        Box(modifier = Modifier.weight(1f).padding(bottom = Theme.spacing.large)) {
            content()
        }
    }
}

@Composable
private fun TimelineLine(visible: Boolean, modifier: Modifier) {
    if (visible) {
        Box(modifier = modifier.width(TimelineSizes.Line).background(Theme.backgrounds.separator))
    } else {
        Spacer(modifier = modifier.width(TimelineSizes.Line))
    }
}

object TimelineSizes {
    // The date's column, which a placeholder lines up with
    val DateWidth = 40.dp
    internal val DateTop = 6.dp
    internal val LineAboveDot = 27.dp
    internal val Dot = 8.dp
    val Line = 2.dp
}

@Preview
@Composable
private fun AppTimelineItemPreview() {
    Theme {
        Column {
            listOf(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 5)).forEachIndexed { index, day ->
                AppTimelineItem(date = day, first = index == 0, last = index == 1) {
                    AppListGroup {
                        row { AppListItem(title = "Push", subtitle = "1:05 h • 4,250 kg") }
                    }
                }
            }
        }
    }
}
