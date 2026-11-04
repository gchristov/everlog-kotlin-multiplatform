package com.everlog.android.home.weektab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everlog.android.common.compose.elements.AppBarHeader
import com.everlog.android.common.compose.elements.AppSectionHeader
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.home.data.HomeNotificationTap

// What the Week tab opens, which the fragment wires to the Home screen and the navigator
class WeekTabActions(
    val showStatistics: () -> Unit,
    val showSettings: () -> Unit,
    val showStartWorkout: () -> Unit,
    val open: (HomeNotificationTap) -> Unit,
)

@Composable
internal fun WeekTabScreen(viewModel: WeekTabViewModel, actions: WeekTabActions) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WeekTabContent(
        state = state,
        actions = actions,
        onBannerClose = viewModel::onBannerClosed,
        onBannerTap = { actions.open(viewModel.onBannerTapped()) },
    )
}

@Composable
internal fun WeekTabContent(
    state: WeekTabState,
    actions: WeekTabActions,
    onBannerClose: () -> Unit,
    onBannerTap: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        AppBarHeader(title = stringResource(DesignR.string.home_week))
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = WeekTabSizes.BottomPadding),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
        ) {
            val margin = Modifier.padding(horizontal = Theme.spacing.large)
            state.banner?.let {
                WeekTabBannerCard(banner = it, onTap = onBannerTap, onClose = onBannerClose, modifier = margin)
            }
            WeekDays(state = state, onClick = actions.showStatistics, modifier = margin)
            if (state.loading) {
                WeekStatsLoading(modifier = margin)
            } else {
                AppSectionHeader(text = stringResource(DesignR.string.statistics_title), modifier = margin)
                WeekStatCards(
                    state = state,
                    onGoalClick = actions.showSettings,
                    onStatsClick = actions.showStatistics,
                    contentPadding = PaddingValues(horizontal = Theme.spacing.large),
                )
            }
            if (state.empty) {
                EmptyWeekCard(today = state.today, onStart = actions.showStartWorkout, modifier = margin)
            }
        }
    }
}

internal object WeekTabSizes {
    // Room for Home's start workout button over the end of the list
    val BottomPadding = 120.dp
    val StatCardWidth = 154.dp
    val StatCardHeight = 170.dp
    val DayWidth = 36.dp
    val GoalTrackWidth = 4.dp
    val GoalProgressWidth = 8.dp
    val BannerImage = 52.dp
    val StatIcon = 20.dp
    val StartIcon = 24.dp
    val DayCorner = 4.dp
    val LoadingHeaderWidth = 120.dp
    val LoadingHeaderHeight = 16.dp
    val CardElevation = 2.dp
}
