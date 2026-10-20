package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.elements.AppBottomSheet
import com.everlog.ui.design.elements.AppSecondaryButton
import com.everlog.ui.design.elements.list.AppListGroup
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme

// A sheet of options like the workout screen's: picking a row closes the sheet, then says what was picked
@Composable
internal fun DesignSystemBottomSheet() {
    var open by rememberSaveable { mutableStateOf(false) }
    var picked by rememberSaveable { mutableStateOf("Nothing yet") }

    ShowcasePage(title = "Bottom sheet") {
        group(key = "sheet", header = { "Options in a sheet, closed by picking one" }) {
            items(count = 1, key = { it }) {
                AppListItem(title = "Picked", subtitle = picked)
            }
        }
        item(key = "open") {
            AppSecondaryButton(modifier = Modifier.fillMaxWidth(), onClick = { open = true }, text = "Show sheet")
        }
    }

    if (open) {
        AppBottomSheet(onDismissRequest = { open = false }, title = "Workout options") {
            AppListGroup {
                row { AppListItem(title = "Rest timer", subtitle = "After each set", onClick = { dismiss { picked = "Rest timer" } }) }
                row { AppListItem(title = "Edit workout", subtitle = "Reorder or delete exercises", onClick = { dismiss { picked = "Edit workout" } }) }
            }
            AppListGroup {
                row {
                    AppListItem(
                        title = "Discard workout",
                        titleColor = Theme.contentColors.destructive,
                        onClick = { dismiss { picked = "Discard workout" } },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DesignSystemBottomSheetPreview() {
    Theme {
        DesignSystemBottomSheet()
    }
}
