package com.everlog.ui.activities.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.R
import com.everlog.ui.design.elements.AppMenuItem
import com.everlog.ui.design.elements.AppOverflowMenu
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme

// The overflow menu in the app bar, as on the exercise picker, and on a row, as on a plan's days
@Composable
internal fun DesignSystemMenus(onButtonClick: () -> Unit) {
    val add = ImageVector.vectorResource(R.drawable.ic_add)
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
