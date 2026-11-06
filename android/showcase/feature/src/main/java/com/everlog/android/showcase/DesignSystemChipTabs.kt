package com.everlog.android.showcase

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.AppChipTabs
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.theme.Theme

private val Tabs = listOf("Info", "Statistics", "History")

@Composable
internal fun DesignSystemChipTabs() {
    ShowcasePage(title = "Chip tabs") {
        group(key = "pager", header = { "Over pages, swiped or picked" }) {}
        item(key = "pagerExample") {
            val pager = rememberPagerState { Tabs.size }
            Column {
                // Already at the screen margin, as the list pads its items
                AppChipTabs(tabs = Tabs, pager = pager, contentPadding = PaddingValues(bottom = Theme.spacing.large))
                HorizontalPager(state = pager) { page ->
                    AppSurface(modifier = Modifier.fillMaxWidth().height(ExampleHeight)) {
                        Box(modifier = Modifier.fillMaxWidth().height(ExampleHeight), contentAlignment = Alignment.Center) {
                            AppText(text = "${Tabs[page]} page", color = Theme.contentColors.secondary)
                        }
                    }
                }
            }
        }
    }
}

private val ExampleHeight = 160.dp

@Preview
@Composable
private fun DesignSystemChipTabsPreview() {
    Theme {
        DesignSystemChipTabs()
    }
}
