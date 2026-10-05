package com.everlog.ui.activities.designsystem

import androidx.compose.runtime.Composable
import com.everlog.ui.design.elements.AppBar
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.list.AppGroupedList
import com.everlog.ui.design.elements.list.AppGroupedListScope

// A showcase page: a back button and the page's title, then its examples as a grouped list.
// Sections without rows are a header-only group followed by their content.
@Composable
internal fun ShowcasePage(
    title: String,
    footer: @Composable () -> Unit = {},
    content: AppGroupedListScope.() -> Unit,
) {
    AppScreen(
        topBar = {
            AppBar(
                title = title,
                showBack = true,
            )
        },
        footer = footer,
    ) { contentPadding ->
        AppGroupedList(
            contentPadding = contentPadding,
            content = content,
        )
    }
}
