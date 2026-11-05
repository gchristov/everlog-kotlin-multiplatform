package com.everlog.android.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.elements.AppExample
import com.everlog.android.common.compose.elements.AppExampleLabel
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.list.AppListGroup
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme

// An empty screen's faded example of what it becomes, as on the workout screen, the template editor and the Activity
// tab's statistics and history
@Composable
internal fun DesignSystemExamples() {
    ShowcasePage(title = "Examples") {
        group(key = "cards", header = { "Sample numbers, faded under the label" }) {}
        item(key = "cardsExample") {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium)) {
                AppExampleLabel()
                AppExample(modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                        SampleNumber("3", "workouts done", Modifier.weight(1f))
                        SampleNumber("8,450 kg", "volume lifted", Modifier.weight(1f))
                    }
                }
            }
        }
        group(key = "list", header = { "Sample rows, which can't be tapped" }) {}
        item(key = "listExample") {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium)) {
                AppExampleLabel()
                AppExample(modifier = Modifier.fillMaxWidth()) {
                    AppListGroup {
                        listOf("Legs", "Push").forEach { name -> row { AppListItem(title = name, subtitle = "0:52 h • 6,100 kg", onClick = {}) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleNumber(value: String, label: String, modifier: Modifier) {
    AppSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            AppText(text = value, style = Theme.typography.heading)
            AppText(text = label, style = Theme.typography.caption, color = Theme.contentColors.secondary)
        }
    }
}

@Preview
@Composable
private fun DesignSystemExamplesPreview() {
    Theme {
        DesignSystemExamples()
    }
}
