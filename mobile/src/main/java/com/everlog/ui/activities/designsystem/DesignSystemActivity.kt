package com.everlog.ui.activities.designsystem

import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppBar
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.elements.list.AppGroupedList
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.mvvm.createViewModelFactory
import kotlinx.coroutines.Dispatchers

// Debug-only showcase of the Compose design system, opened from Settings. A list of pages, each in
// its own file: the components, and screens to try behaviours that need a whole screen (e.g. app
// bar scrolling).
class DesignSystemActivity : CommonComposeActivity() {
    private val viewModel by viewModels<DesignSystemViewModel> {
        createViewModelFactory { DesignSystemViewModel(dispatcher = Dispatchers.Main) }
    }

    @Composable
    override fun Content() = DesignSystemScreen(viewModel = viewModel)
}

internal enum class DesignSystemPage {
    Home,
    Components,
    ScrollBehaviors,
    ScrollAwayList,
    ScrollAwayPage,
    ScrollAwayShortPage,
    PinnedHeader,
}

// The pages open on top of each other in this one activity. Back (the app bar's or the system's)
// returns to the previous page, and from the first one closes the showcase.
@Composable
internal fun DesignSystemScreen(viewModel: DesignSystemViewModel) {
    val pages = rememberSaveable(saver = PagesSaver) { mutableListOf(DesignSystemPage.Home).toMutableStateList() }
    val open: (DesignSystemPage) -> Unit = { pages.add(it) }

    BackHandler(enabled = pages.size > 1) {
        pages.removeAt(pages.lastIndex)
    }

    when (pages.last()) {
        DesignSystemPage.Home -> DesignSystemHome(onOpen = open)
        DesignSystemPage.Components -> DesignSystemComponents(viewModel = viewModel)
        DesignSystemPage.ScrollBehaviors -> ScrollBehaviors(onOpen = open)
        DesignSystemPage.ScrollAwayList -> ScrollAwayListExample()
        DesignSystemPage.ScrollAwayPage -> ScrollAwayPageExample()
        DesignSystemPage.ScrollAwayShortPage -> ScrollAwayShortPageExample()
        DesignSystemPage.PinnedHeader -> PinnedHeaderExample()
    }
}

private val PagesSaver = listSaver<SnapshotStateList<DesignSystemPage>, String>(
    save = { pages -> pages.map { it.name } },
    restore = { names -> names.map { DesignSystemPage.valueOf(it) }.toMutableStateList() },
)

@Composable
private fun DesignSystemHome(onOpen: (DesignSystemPage) -> Unit) {
    AppScreen(
        topBar = {
            AppBar(
                title = "Design system",
                showBack = true,
            )
        },
    ) { contentPadding ->
        AppGroupedList(contentPadding = contentPadding) {
            group(key = "pages") {
                items(count = 2, key = { it }) { index ->
                    when (index) {
                        0 -> AppListItem(
                            title = "Components",
                            subtitle = "Typography, icons, buttons, lists, cards and dialogs",
                            onClick = { onOpen(DesignSystemPage.Components) },
                        )
                        else -> AppListItem(
                            title = "App bar scroll behaviours",
                            subtitle = "Whole screens to try how an app bar's header scrolls",
                            onClick = { onOpen(DesignSystemPage.ScrollBehaviors) },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DesignSystemHomePreview() {
    Theme {
        DesignSystemHome(onOpen = {})
    }
}
