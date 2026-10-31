package com.everlog.android.showcase

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import com.everlog.android.common.compose.elements.AppBar
import com.everlog.android.common.compose.elements.AppScreen
import com.everlog.android.common.compose.elements.list.AppGroupedList
import com.everlog.android.common.compose.elements.list.AppGroupedListScope

// A showcase page: a back button and the page's title, then its examples as a grouped list.
// Sections without rows are a header-only group followed by their content.
@Composable
internal fun ShowcasePage(
    title: String,
    actions: (@Composable RowScope.() -> Unit)? = null,
    footer: @Composable () -> Unit = {},
    content: AppGroupedListScope.() -> Unit,
) {
    AppScreen(
        topBar = {
            AppBar(
                title = title,
                showBack = true,
                actions = actions,
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
