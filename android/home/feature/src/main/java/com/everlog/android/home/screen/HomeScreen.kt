package com.everlog.android.home.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.android.common.compose.components.broadcast.LocalBroadcastEffect
import com.everlog.android.common.compose.elements.AppBottomBar
import com.everlog.android.common.compose.elements.AppBottomBarItem
import com.everlog.android.common.compose.elements.AppFloatingButton
import com.everlog.android.common.compose.elements.AppSnackbarHost
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.home.R
import com.everlog.android.home.activity.ActivityTabPages
import com.everlog.android.home.activity.ActivityTabRoute
import com.everlog.android.home.weektab.WeekTabHomeActions
import com.everlog.android.home.weektab.WeekTabRoute
import com.everlog.android.home.workoutstab.WorkoutsTabRoute
import com.everlog.android.plan.data.PlanBroadcasts
import com.everlog.android.settings.ui.tab.SettingsTabRoute
import kotlinx.coroutines.launch

/**
 * Home: the four tabs in a pager to swipe through or pick from the bottom bar, each tab's floating button over it,
 * and the app update snackbar above the bar, the button moving up out of its way.
 */
@Composable
internal fun HomeScreen(
    viewModel: HomeViewModel,
    sheetActions: HomeSheetActions,
    onRestartForUpdate: () -> Unit,
    // A tab's content: the routes, or stand-ins in a test
    tab: @Composable (tab: HomeTab, shown: Boolean, links: HomeTabLinks) -> Unit = ::HomeTabRoute,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pager = rememberPagerState(initialPage = state.tab.ordinal) { HomeTab.entries.size }
    val activityPager = rememberPagerState { ActivityTabPages.Count }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.tab) {
        if (pager.currentPage != state.tab.ordinal) pager.animateScrollToPage(state.tab.ordinal)
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { viewModel.onTabSelected(HomeTab.entries[it]) }
    }
    BackHandler(enabled = state.tab != HomeTab.Week) { viewModel.onBack() }
    LocalBroadcastEffect(PlanBroadcasts.CURRENT_PLAN_STARTED) { viewModel.onPlanStarted() }

    val updateMessage = stringResource(R.string.home_update_ready)
    val updateAction = stringResource(R.string.home_update_restart)
    LaunchedEffect(state.updateReady) {
        if (!state.updateReady) return@LaunchedEffect
        // Marked shown only once it's gone: changing the key first would cancel the snackbar before it showed
        val result = snackbar.showSnackbar(message = updateMessage, actionLabel = updateAction, duration = SnackbarDuration.Indefinite)
        viewModel.onUpdateShown()
        if (result == SnackbarResult.ActionPerformed) onRestartForUpdate()
    }

    val links = HomeTabLinks(
        week = WeekTabHomeActions(
            showStatistics = {
                viewModel.onTabSelected(HomeTab.Activity)
                scope.launch { activityPager.scrollToPage(ActivityTabPages.Statistics) }
            },
            showSettings = { viewModel.onTabSelected(HomeTab.Settings) },
            showPlans = { viewModel.onTabSelected(HomeTab.Workouts) },
            showStartMenu = viewModel::onStartFromEmptyWeek,
            onWeekEmpty = viewModel::onWeekEmpty,
        ),
        activityPager = activityPager,
    )
    Column(modifier = Modifier.fillMaxSize().background(Theme.backgrounds.primary)) {
        // The bottom bar keeps clear of the navigation bar, so the tabs above it don't
        Box(modifier = Modifier.weight(1f).consumeWindowInsets(WindowInsets.navigationBars)) {
            // Every tab stays alive, as in the old Home's pager, so going back to one keeps its place
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().testTag(HomeTags.Pager), beyondViewportPageCount = HomeTab.entries.size) { page ->
                // Once the pager settles, so tabs passed on the way to another don't count as shown
                tab(HomeTab.entries[page], pager.settledPage == page, links)
            }
            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.End) {
                FloatingButton(button = state.button, snackbarShown = snackbar.currentSnackbarData != null, onClick = viewModel::onButtonTapped)
                AppSnackbarHost(hostState = snackbar)
            }
        }
        AppBottomBar(
            items = HomeTab.entries.map { it.barItem() },
            selected = state.tab.ordinal,
            onSelected = { viewModel.onTabSelected(HomeTab.entries[it]) },
        )
    }
    HomeSheets(state = state, viewModel = viewModel, actions = sheetActions)
}

// What a tab can ask of Home: the Week tab's links, and the Activity tab's pages, which the Week tab opens on Statistics
class HomeTabLinks(val week: WeekTabHomeActions, val activityPager: PagerState)

@Composable
internal fun HomeTabRoute(tab: HomeTab, shown: Boolean, links: HomeTabLinks) {
    when (tab) {
        HomeTab.Week -> WeekTabRoute(shown = shown, home = links.week, bottomPadding = HomeSizes.TabBottomPadding)
        HomeTab.Workouts -> WorkoutsTabRoute(shown = shown, bottomPadding = HomeSizes.TabBottomPadding)
        HomeTab.Activity -> ActivityTabRoute(shown = shown, pager = links.activityPager, bottomPadding = HomeSizes.TabBottomPadding)
        HomeTab.Settings -> SettingsTabRoute(shown = shown, bottomPadding = HomeSizes.TabBottomPadding)
    }
}

@Composable
private fun FloatingButton(button: HomeButton?, snackbarShown: Boolean, onClick: () -> Unit) {
    // The last button, so its label stays while it fades out
    var shown by remember { mutableStateOf(button ?: HomeButton.StartWorkout) }
    if (button != null) shown = button
    AppFloatingButton(
        modifier = Modifier
            .padding(
                start = Theme.spacing.large,
                top = Theme.spacing.large,
                end = Theme.spacing.large,
                bottom = if (snackbarShown) Theme.spacing.large - HomeSizes.SnackbarMargin else Theme.spacing.large,
            )
            .testTag(HomeTags.Button),
        text = stringResource(if (shown == HomeButton.New) R.string.home_new else R.string.home_start_workout),
        icon = ImageVector.vectorResource(DesignR.drawable.ic_add),
        onClick = onClick,
        visible = button != null,
    )
}

@Composable
private fun HomeTab.barItem(): AppBottomBarItem = when (this) {
    HomeTab.Week -> AppBottomBarItem(stringResource(DesignR.string.home_week), ImageVector.vectorResource(DesignR.drawable.ic_home), HomeTags.tab(this))
    HomeTab.Workouts -> AppBottomBarItem(stringResource(R.string.workouts_tab_title), ImageVector.vectorResource(DesignR.drawable.ic_barbell), HomeTags.tab(this))
    HomeTab.Activity -> AppBottomBarItem(stringResource(DesignR.string.home_activity), ImageVector.vectorResource(DesignR.drawable.ic_insights), HomeTags.tab(this))
    HomeTab.Settings -> AppBottomBarItem(stringResource(DesignR.string.settings_title), ImageVector.vectorResource(DesignR.drawable.ic_settings), HomeTags.tab(this))
}

internal object HomeSizes {
    // Every tab's room at the end of its list, so its content scrolls clear of the floating button: the button's
    // margin and height, and about the old Home's gap above it
    val TabBottomPadding = 120.dp

    // Material's margin around a snackbar, which already keeps the button off it
    val SnackbarMargin = 12.dp
}
