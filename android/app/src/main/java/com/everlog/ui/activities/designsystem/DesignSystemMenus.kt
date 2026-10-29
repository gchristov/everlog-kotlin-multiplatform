package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.R
import com.everlog.android.common.compose.elements.AppChip
import com.everlog.android.common.compose.elements.AppMenu
import com.everlog.android.common.compose.elements.AppMenuItem
import com.everlog.android.common.compose.elements.AppOverflowMenu
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

// The overflow menu in the app bar, as on the exercise picker, and on a row, as on a plan's days;
// and a chip picking one of several options, as statistics' time range
@Composable
internal fun DesignSystemMenus(onButtonClick: () -> Unit) {
    val ranges = listOf("Today", "This week", "This month", "This year", "Overall")
    var range by remember { mutableStateOf("This week") }
    var rangeOpen by remember { mutableStateOf(false) }
    val add = ImageVector.vectorResource(DesignR.drawable.ic_add)
    val copy = ImageVector.vectorResource(R.drawable.ic_copy)
    val delete = ImageVector.vectorResource(R.drawable.ic_delete_black)
    ShowcasePage(
        title = "Menus",
        actions = {
            AppOverflowMenu(
                items = listOf(
                    AppMenuItem(text = "Create an exercise", onClick = onButtonClick, icon = add),
                    AppMenuItem(text = "Send feedback", onClick = onButtonClick),
                ),
            )
        },
    ) {
        group(key = "appBar", header = { "In the app bar: tap ⋮ at the top" }) {}
        group(key = "choice", header = { "Picking one of several, ticked" }) {}
        item(key = "choiceExample") {
            Box {
                AppChip(text = range, onClick = { rangeOpen = true }, dropdown = true)
                AppMenu(
                    expanded = rangeOpen,
                    onDismissRequest = { rangeOpen = false },
                    items = ranges.map { AppMenuItem(text = it, onClick = { range = it }, selected = it == range) },
                )
            }
        }
        group(key = "row", header = { "On a row" }) {
            items(count = 1, key = { it }) {
                AppListItem(
                    title = "Week 1",
                    subtitle = "3 workouts, 4 rest days",
                    trailing = {
                        AppOverflowMenu(
                            items = listOf(
                                AppMenuItem(text = "Duplicate", onClick = onButtonClick, icon = copy),
                                AppMenuItem(text = "Delete", onClick = onButtonClick, icon = delete),
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun DesignSystemMenusPreview() {
    Theme {
        DesignSystemMenus(onButtonClick = {})
    }
}
