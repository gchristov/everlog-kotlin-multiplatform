package com.everlog.android.showcase

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.elements.AppBlurBox
import com.everlog.android.common.compose.elements.AppChip
import com.everlog.android.common.compose.elements.AppChipStyle
import com.everlog.android.common.compose.elements.AppMenu
import com.everlog.android.common.compose.elements.AppMenuItem
import com.everlog.android.common.compose.elements.AppOverflowMenu
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

// The overflow menu in the app bar, as on the exercise picker, and on a row, as on a plan's days;
// and a chip picking one of several options, as statistics' time range, also floating over content, as the history's
// calendar or list picker
@Composable
internal fun DesignSystemMenus(onButtonClick: () -> Unit) {
    val ranges = listOf("Today", "This week", "This month", "This year", "Overall")
    var range by remember { mutableStateOf("This week") }
    var rangeOpen by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf("Calendar") }
    var viewOpen by remember { mutableStateOf(false) }
    val add = ImageVector.vectorResource(DesignR.drawable.ic_add)
    val copy = ImageVector.vectorResource(DesignR.drawable.ic_copy)
    val delete = ImageVector.vectorResource(DesignR.drawable.ic_delete_black)
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
        group(key = "floating", header = { "Floating over content, which blurs behind it" }) {}
        item(key = "floatingExample") {
            AppBlurBox(
                modifier = Modifier.fillMaxWidth(),
                overlay = {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        AppChip(text = view, onClick = { viewOpen = true }, style = AppChipStyle.Action, dropdown = true, floating = true)
                        AppMenu(
                            expanded = viewOpen,
                            onDismissRequest = { viewOpen = false },
                            items = listOf("Calendar", "List").map { AppMenuItem(text = it, onClick = { view = it }, selected = it == view) },
                        )
                    }
                },
            ) {
                AppListGroup {
                    listOf("Legs", "Push", "Pull").forEach { name -> row { AppListItem(title = name, subtitle = "0:45 h • 4,200 kg") } }
                }
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
