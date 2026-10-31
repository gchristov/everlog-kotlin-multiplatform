package com.everlog.android.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.AppChip
import com.everlog.android.common.compose.elements.AppChipStyle
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.search.AppSearchBar
import com.everlog.android.common.compose.theme.Theme

// The search bar, plain and with content before the text (a screen's own filter tokens), and chips, plain and selected
@Composable
internal fun DesignSystemSearch(onButtonClick: () -> Unit) {
    var plain by remember { mutableStateOf("") }
    var withTokens by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("Info") }
    ShowcasePage(title = "Search and filters") {
        group(key = "search", header = { "Search bar" }) {}
        item(key = "searchExample") {
            AppSearchBar(text = plain, onTextChange = { plain = it }, placeholder = "Search 168 exercises")
        }
        group(key = "searchContent", header = { "With content before the text, e.g. filter tokens" }) {}
        item(key = "searchContentExample") {
            AppSearchBar(text = withTokens, onTextChange = { withTokens = it }, placeholder = "Add a filter or name") {
                SampleToken("Muscle is Chest")
                SampleToken("Equipment is Barbell, Dumbbell")
            }
        }
        group(key = "chips", header = { "Chips" }) {}
        item(key = "chipsExample") {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                AppChip(text = "Muscle", onClick = onButtonClick, dropdown = true)
                AppChip(text = "Equipment", onClick = onButtonClick, dropdown = true)
                AppChip(text = "Recent", onClick = onButtonClick)
                AppChip(text = "Clear all", onClick = onButtonClick, style = AppChipStyle.Action)
            }
        }
        group(key = "chipTabs", header = { "Selected, e.g. tabs (TabsPage is the XML twin)" }) {}
        item(key = "chipTabsExample") {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                listOf("Info", "Statistics", "History").forEach {
                    AppChip(text = it, onClick = { tab = it }, selected = tab == it)
                }
            }
        }
    }
}

// Stands in for a screen's own token
@Composable
private fun SampleToken(text: String) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Theme.backgrounds.surfaceRaised)
            .padding(horizontal = Theme.spacing.small),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = text, style = Theme.typography.button, maxLines = 1)
    }
}

@Preview
@Composable
private fun DesignSystemSearchPreview() {
    Theme {
        DesignSystemSearch(onButtonClick = {})
    }
}
