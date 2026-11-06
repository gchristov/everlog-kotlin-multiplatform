package com.everlog.android.showcase

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.elements.AppTimelineItem
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import org.threeten.bp.LocalDate

@Composable
internal fun DesignSystemTimeline() {
    ShowcasePage(title = "Timeline") {
        group(key = "days", header = { "Days, oldest first, each with its own content" }) {}
        item(key = "daysExample") {
            val days = listOf(
                LocalDate.of(2026, 10, 31) to listOf("Push" to "58 min • 4,250 kg"),
                LocalDate.of(2026, 11, 2) to listOf("Legs" to "1:02 h • 6,120 kg", "Core" to "15 min"),
                LocalDate.of(2026, 11, 4) to listOf("Pull" to "55 min • 3,980 kg"),
            )
            Column {
                days.forEachIndexed { index, (day, workouts) ->
                    AppTimelineItem(date = day, first = index == 0, last = index == days.lastIndex) {
                        AppListGroup {
                            workouts.forEach { (name, summary) -> row { AppListItem(title = name, subtitle = summary) } }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DesignSystemTimelinePreview() {
    Theme {
        DesignSystemTimeline()
    }
}
