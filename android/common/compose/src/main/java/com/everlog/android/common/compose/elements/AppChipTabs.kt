package com.everlog.android.common.compose.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.theme.Theme
import kotlinx.coroutines.launch

/**
 * Tabs as chips in a row at the screen margin, the one shown picked, e.g. under an [AppBar] (its header) over pages
 * the screen swipes between.
 *
 * @param contentPadding The screen margin at the sides and a group's spacing below, by default
 * @param tabModifier Per tab, e.g. a test tag
 */
@Composable
fun AppChipTabs(
    tabs: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = AppChipTabsDefaults.contentPadding(),
    tabModifier: (Int) -> Modifier = { Modifier },
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        tabs.forEachIndexed { index, tab ->
            AppChip(text = tab, selected = index == selected, onClick = { onSelected(index) }, modifier = tabModifier(index))
        }
    }
}

// The tabs of a pager: the page shown is picked, and picking a tab scrolls to its page
@Composable
fun AppChipTabs(
    tabs: List<String>,
    pager: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = AppChipTabsDefaults.contentPadding(),
    tabModifier: (Int) -> Modifier = { Modifier },
) {
    val scope = rememberCoroutineScope()
    AppChipTabs(
        tabs = tabs,
        selected = pager.currentPage,
        onSelected = { scope.launch { pager.animateScrollToPage(it) } },
        modifier = modifier,
        contentPadding = contentPadding,
        tabModifier = tabModifier,
    )
}

object AppChipTabsDefaults {
    @Composable
    fun contentPadding() = PaddingValues(start = Theme.spacing.large, end = Theme.spacing.large, bottom = Theme.spacing.large)
}

@Preview
@Composable
private fun AppChipTabsPreview() {
    Theme {
        AppChipTabs(tabs = listOf("Info", "Statistics", "History"), selected = 1, onSelected = {})
    }
}
