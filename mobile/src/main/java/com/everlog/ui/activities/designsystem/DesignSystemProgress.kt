package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.elements.AppCircularProgressIndicator
import com.everlog.ui.design.elements.AppLoadingScreen
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme

@Composable
internal fun DesignSystemProgress(onOpen: (DesignSystemPage) -> Unit) {
    ShowcasePage(title = "Progress") {
        group(key = "indicator", header = { "Circular progress indicator" }) {}
        item(key = "indicatorExample") {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AppCircularProgressIndicator()
            }
        }
        group(key = "screens", header = { "Whole screens" }) {
            items(count = 1, key = { "loadingScreen" }) {
                AppListItem(
                    title = "Loading screen",
                    subtitle = "Covers the screen while something loads. Back returns here.",
                    onClick = { onOpen(DesignSystemPage.LoadingScreen) },
                )
            }
        }
    }
}

@Composable
internal fun LoadingScreenExample() {
    AppLoadingScreen()
}

@Preview
@Composable
private fun DesignSystemProgressPreview() {
    Theme {
        DesignSystemProgress(onOpen = {})
    }
}
