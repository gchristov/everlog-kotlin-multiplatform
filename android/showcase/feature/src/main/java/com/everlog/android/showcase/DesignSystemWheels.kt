package com.everlog.android.showcase

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.elements.AppBottomSheet
import com.everlog.android.common.compose.elements.AppButton
import com.everlog.android.common.compose.elements.AppSecondaryButton
import com.everlog.android.common.compose.elements.AppWheel
import com.everlog.android.common.compose.elements.AppWheelColumn
import com.everlog.android.common.compose.theme.Theme

private val Goals = (1..7).map { if (it == 1) "1 workout" else "$it workouts" }
private val Wholes = (1..20).map { "$it" }
private val Quarters = listOf(".00", ".25", ".50", ".75")

// One wheel on the page, and two side by side in a bottom sheet as Settings picks a weight increment
@Composable
internal fun DesignSystemWheels() {
    var goal by rememberSaveable { mutableIntStateOf(2) }
    var whole by rememberSaveable { mutableIntStateOf(0) }
    var quarter by rememberSaveable { mutableIntStateOf(1) }
    var open by rememberSaveable { mutableStateOf(false) }

    ShowcasePage(title = "Wheels") {
        group(key = "one", header = { "One column: ${Goals[goal]}" }) {}
        item(key = "oneExample") {
            AppWheel(columns = listOf(AppWheelColumn(Goals, goal, { goal = it }, "Workouts a week")))
        }
        group(key = "two", header = { "Two columns and a unit, in a sheet: ${Wholes[whole]}${Quarters[quarter]} kg" }) {}
        item(key = "twoExample") {
            AppSecondaryButton(modifier = Modifier.fillMaxWidth(), onClick = { open = true }, text = "Pick a weight increment")
        }
    }

    if (open) {
        var editedWhole by rememberSaveable { mutableIntStateOf(whole) }
        var editedQuarter by rememberSaveable { mutableIntStateOf(quarter) }
        AppBottomSheet(onDismissRequest = { open = false }, title = "Weight increment") {
            AppWheel(
                columns = listOf(
                    AppWheelColumn(Wholes, editedWhole, { editedWhole = it }, "Whole kilograms"),
                    AppWheelColumn(Quarters, editedQuarter, { editedQuarter = it }, "Quarters"),
                ),
                suffix = "kg",
            )
            AppButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { dismiss { whole = editedWhole; quarter = editedQuarter } },
                text = "Done",
            )
        }
    }
}

@Preview
@Composable
private fun DesignSystemWheelsPreview() {
    Theme {
        DesignSystemWheels()
    }
}
