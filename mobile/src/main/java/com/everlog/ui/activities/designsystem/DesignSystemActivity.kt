package com.everlog.ui.activities.designsystem

import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme
import com.everlog.ui.mvvm.createViewModelFactory
import kotlinx.coroutines.Dispatchers

// Debug-only showcase of the Compose design system, opened from Settings. A list of pages, each in
// its own file: the foundations (typography, icons), one page per component type, and screens to
// try behaviours that need a whole screen (e.g. app bar scrolling).
class DesignSystemActivity : CommonComposeActivity() {
    private val viewModel by viewModels<DesignSystemViewModel> {
        createViewModelFactory { DesignSystemViewModel(dispatcher = Dispatchers.Main) }
    }

    @Composable
    override fun Content() = DesignSystemScreen(viewModel = viewModel)
}

internal enum class DesignSystemPage {
    Home,
    Typography,
    Icons,
    Buttons,
    Lists,
    Cards,
    HeroHeader,
    Dialogs,
    Footer,
    Progress,
    ScrollBehaviors,
    ScrollAwayList,
    CollapsingList,
    ScrollAwayPage,
    ScrollAwayShortPage,
    PinnedHeader,
}

// The pages open on top of each other in this one activity. Back (the app bar's or the system's)
// returns to the previous page, and from the first one closes the showcase.
@Composable
internal fun DesignSystemScreen(viewModel: DesignSystemViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onButtonClick = viewModel::onButtonClick
    val pages = rememberSaveable(saver = PagesSaver) { mutableListOf(DesignSystemPage.Home).toMutableStateList() }
    val open: (DesignSystemPage) -> Unit = { pages.add(it) }

    BackHandler(enabled = pages.size > 1) {
        pages.removeAt(pages.lastIndex)
    }

    when (pages.last()) {
        DesignSystemPage.Home -> DesignSystemHome(onOpen = open)
        DesignSystemPage.Typography -> DesignSystemTypography()
        DesignSystemPage.Icons -> DesignSystemIcons(onButtonClick = onButtonClick)
        DesignSystemPage.Buttons -> DesignSystemButtons(buttonClicks = state.buttonClicks, onButtonClick = onButtonClick)
        DesignSystemPage.Lists -> DesignSystemLists(onButtonClick = onButtonClick)
        DesignSystemPage.Cards -> DesignSystemCards(onButtonClick = onButtonClick)
        DesignSystemPage.HeroHeader -> DesignSystemHeroHeader()
        DesignSystemPage.Dialogs -> DesignSystemDialogs()
        DesignSystemPage.Footer -> DesignSystemFooter(onButtonClick = onButtonClick)
        DesignSystemPage.Progress -> DesignSystemProgress()
        DesignSystemPage.ScrollBehaviors -> ScrollBehaviors(onOpen = open)
        DesignSystemPage.ScrollAwayList -> ScrollAwayListExample()
        DesignSystemPage.CollapsingList -> CollapsingListExample()
        DesignSystemPage.ScrollAwayPage -> ScrollAwayPageExample()
        DesignSystemPage.ScrollAwayShortPage -> ScrollAwayShortPageExample()
        DesignSystemPage.PinnedHeader -> PinnedHeaderExample()
    }
}

private val PagesSaver = listSaver<SnapshotStateList<DesignSystemPage>, String>(
    save = { pages -> pages.map { it.name } },
    restore = { names -> names.map { DesignSystemPage.valueOf(it) }.toMutableStateList() },
)

private data class HubEntry(
    val page: DesignSystemPage,
    val title: String,
    val description: String,
)

private val HubSections = listOf(
    "Foundations" to listOf(
        HubEntry(DesignSystemPage.Typography, "Typography", "Text styles, with their sizes and weights"),
        HubEntry(DesignSystemPage.Icons, "Icons", "The app's icons, content colours and icon buttons"),
    ),
    "Components" to listOf(
        HubEntry(DesignSystemPage.Buttons, "Buttons", "Primary, secondary and tertiary, enabled and disabled"),
        HubEntry(DesignSystemPage.Lists, "Lists", "Grouped lists and their rows"),
        HubEntry(DesignSystemPage.Cards, "Cards", "The card surface, with text and actions"),
        HubEntry(DesignSystemPage.HeroHeader, "Hero header", "A large title at the top of a screen's content"),
        HubEntry(DesignSystemPage.Dialogs, "Dialogs", "A title, body text and two actions"),
        HubEntry(DesignSystemPage.Footer, "Footer", "Actions in a card pinned to the bottom, blurring what scrolls behind"),
        HubEntry(DesignSystemPage.Progress, "Progress", "A circular progress indicator"),
    ),
    "Behaviours" to listOf(
        HubEntry(DesignSystemPage.ScrollBehaviors, "App bar scroll behaviours", "Whole screens to try how an app bar's header scrolls"),
    ),
)

@Composable
private fun DesignSystemHome(onOpen: (DesignSystemPage) -> Unit) {
    ShowcasePage(title = "Design system") {
        HubSections.forEach { (section, entries) ->
            group(key = section, header = { section }) {
                items(count = entries.size, key = { entries[it].page }) { index ->
                    val entry = entries[index]
                    AppListItem(
                        title = entry.title,
                        subtitle = entry.description,
                        onClick = { onOpen(entry.page) },
                    )
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
