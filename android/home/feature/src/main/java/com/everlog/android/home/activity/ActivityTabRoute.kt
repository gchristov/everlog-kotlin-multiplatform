package com.everlog.android.home.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.everlog.android.common.compose.components.shown.ShownEffect
import com.everlog.android.common.compose.elements.AppBarHeader
import com.everlog.android.common.compose.elements.AppChipTabs
import com.everlog.android.common.mvp.CommonMvp
import com.everlog.android.history.ui.page.HistoryPageRoute
import com.everlog.android.home.HomeScreenNames
import com.everlog.android.statistics.ui.page.StatisticsPageRoute
import com.everlog.android.common.design.R as DesignR

/**
 * Home's Activity tab: its Statistics and History pages under chips, swiping between them. [pager] is Home's, so the
 * Week tab can open Statistics. Each page reads again when it shows, and History's pages go once another tab shows.
 */
@Composable
fun ActivityTabRoute(shown: Boolean, pager: PagerState) {
    ShownEffect(shown, onShown = { CommonMvp.dependencies.screenViewed(HomeScreenNames.ACTIVITY) })
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        AppBarHeader(title = stringResource(DesignR.string.home_activity))
        AppChipTabs(tabs = listOf(stringResource(DesignR.string.statistics_title), stringResource(DesignR.string.history_title)), pager = pager)
        HorizontalPager(state = pager, modifier = Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
            when (page) {
                ActivityTabPages.Statistics -> StatisticsPageRoute(shown = shown && pager.settledPage == ActivityTabPages.Statistics)
                else -> HistoryPageRoute(shown = shown && pager.settledPage == ActivityTabPages.History, tabShown = shown)
            }
        }
    }
}

// The Activity tab's pages, in order
object ActivityTabPages {
    const val Statistics = 0
    const val History = 1
    const val Count = 2
}
