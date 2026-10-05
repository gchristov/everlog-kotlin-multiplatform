package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.elements.AppBar
import com.everlog.ui.design.elements.AppBarHeader
import com.everlog.ui.design.elements.AppBarScrollBehavior
import com.everlog.ui.design.elements.AppFooter
import com.everlog.ui.design.elements.AppFooterAction
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.AppSurface
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.elements.list.AppGroupedList
import com.everlog.ui.design.elements.list.AppGroupedListScope
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.elements.rememberAppBarScrollBehavior
import com.everlog.ui.design.theme.Theme

private data class ScrollExample(
    val page: DesignSystemPage,
    val title: String,
    val description: String,
)

private val ScrollExamples = listOf(
    ScrollExample(
        page = DesignSystemPage.ScrollAwayList,
        title = "Scrolls away, list",
        description = "A long list. The header leaves first, then the list scrolls. Back down, the list comes back first, then the header.",
    ),
    ScrollExample(
        page = DesignSystemPage.ScrollAwayPage,
        title = "Scrolls away, page with a footer",
        description = "A scrolling column with a pinned footer, like the onboarding welcome on a small screen.",
    ),
    ScrollExample(
        page = DesignSystemPage.ScrollAwayShortPage,
        title = "Scrolls away, page that fits",
        description = "Same behaviour, but the page fits on the screen, so nothing moves.",
    ),
    ScrollExample(
        page = DesignSystemPage.PinnedHeader,
        title = "Pinned",
        description = "No scroll behaviour: the header stays and the list scrolls under it.",
    ),
)

// The app bar's scroll behaviours, each opening a whole screen that uses it
@Composable
internal fun ScrollBehaviors(onOpen: (DesignSystemPage) -> Unit) {
    ShowcasePage(title = "App bar scroll behaviours") {
        group(
            key = "examples",
            footer = { "Drag on the content and on the header itself, slowly and with flings." },
        ) {
            items(count = ScrollExamples.size, key = { ScrollExamples[it].page }) { index ->
                val example = ScrollExamples[index]
                AppListItem(
                    title = example.title,
                    subtitle = example.description,
                    onClick = { onOpen(example.page) },
                )
            }
        }
    }
}

@Composable
internal fun ScrollAwayListExample() {
    val listState = rememberLazyListState()
    val scrollBehavior = rememberAppBarScrollBehavior(canScroll = { listState.canScrollForward })
    ExampleScreen(
        header = "Scrolls away, list",
        scrollBehavior = scrollBehavior,
    ) { contentPadding ->
        AppGroupedList(state = listState, contentPadding = contentPadding) {
            exampleRows(count = 30)
        }
    }
}

@Composable
internal fun ScrollAwayPageExample() {
    val scrollState = rememberScrollState()
    val scrollBehavior = rememberAppBarScrollBehavior(canScroll = { scrollState.canScrollForward })
    ExampleScreen(
        header = "Scrolls away, page with a footer",
        scrollBehavior = scrollBehavior,
        footer = {
            AppFooter(actions = listOf(AppFooterAction(text = "Footer action", onClick = {})))
        },
    ) { contentPadding ->
        ExampleCards(
            count = 8,
            modifier = Modifier.verticalScroll(scrollState),
            contentPadding = contentPadding,
        )
    }
}

@Composable
internal fun ScrollAwayShortPageExample() {
    val scrollState = rememberScrollState()
    val scrollBehavior = rememberAppBarScrollBehavior(canScroll = { scrollState.canScrollForward })
    ExampleScreen(
        header = "Scrolls away, page that fits",
        scrollBehavior = scrollBehavior,
    ) { contentPadding ->
        ExampleCards(
            count = 2,
            modifier = Modifier.verticalScroll(scrollState),
            contentPadding = contentPadding,
        )
    }
}

@Composable
internal fun PinnedHeaderExample() {
    ExampleScreen(
        header = "Pinned",
        scrollBehavior = null,
    ) { contentPadding ->
        AppGroupedList(contentPadding = contentPadding) {
            exampleRows(count = 30)
        }
    }
}

// A screen with a back button and a header in the app bar, scrolling with [scrollBehavior]
@Composable
private fun ExampleScreen(
    header: String,
    scrollBehavior: AppBarScrollBehavior?,
    footer: @Composable () -> Unit = {},
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {
    AppScreen(
        modifier = scrollBehavior?.let { Modifier.nestedScroll(it.nestedScrollConnection) } ?: Modifier,
        topBar = {
            AppBar(
                showBack = true,
                header = {
                    AppBarHeader(
                        eyebrow = "Scroll behaviour",
                        title = header,
                        body = "A header in the app bar, with an eyebrow and a line of body text under the title.",
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        footer = footer,
        content = content,
    )
}

private fun AppGroupedListScope.exampleRows(count: Int) {
    group(key = "rows") {
        items(count = count, key = { it }) { index ->
            AppListItem(
                title = "Row ${index + 1}",
                subtitle = "Some content to scroll through",
            )
        }
    }
}

@Composable
private fun ExampleCards(
    count: Int,
    modifier: Modifier,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .padding(horizontal = Theme.spacing.large)
            .padding(bottom = contentPadding.calculateBottomPadding() + Theme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        repeat(count) { index ->
            AppSurface(modifier = Modifier.fillMaxWidth()) {
                AppText(text = "Card ${index + 1}", style = Theme.typography.heading)
                AppText(
                    text = "A few lines of body text, so there's something to read while the page scrolls under the header.",
                    style = Theme.typography.body,
                    color = Theme.contentColors.secondary,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ScrollBehaviorsPreview() {
    Theme {
        ScrollBehaviors(onOpen = {})
    }
}
