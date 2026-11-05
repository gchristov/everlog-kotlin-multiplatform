package com.everlog.ui.activities.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager.widget.ViewPager
import com.everlog.R
import com.everlog.android.history.ui.page.HistoryPageTags
import com.everlog.android.home.weektab.WeekTabTags
import com.everlog.android.statistics.ui.page.StatisticsPageTags
import com.everlog.managers.RemoteConfigManager
import com.everlog.testutil.E2EAccount
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * Home's Activity tab end to end, through the real Home screen: the Compose statistics and history pages Remote
 * Config's statistics_v2 and history_v2 pick (on in the dev project), each read through DataSync for the E2E
 * account, and the Week tab opening the statistics page whichever page was shown last. StatisticsPageScenarioTest and
 * HistoryPageScenarioTest cover the pages themselves. It reads the account and saves nothing.
 */
@RunWith(AndroidJUnit4::class)
class ActivityTabActivityTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(3, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun setUp() {
        E2EAccount.signIn()
        // A new install only has the defaults until its first fetch, and the Activity tab picks its pages when it starts
        instrumentation.runOnMainSync { RemoteConfigManager.manager.refreshAppConfig() }
        val deadline = System.currentTimeMillis() + LoadMillis
        while (!flagsOn() && System.currentTimeMillis() < deadline) Thread.sleep(PollMillis)
        assertThat(flagsOn()).isTrue()
    }

    @Test
    fun theActivityTabShowsBothPagesAndTheWeekOpensStatistics() {
        val home = ActivityScenario.launch(HomeActivity::class.java)
        home.onActivity { it.findViewById<ViewPager>(R.id.pager).currentItem = ActivityTab }

        // Each page has loaded once its content shows instead of the shimmer: the account's, or the example when the
        // week has no workouts yet
        compose.waitUntil(LoadMillis) { exists(StatisticsPageTags.Workouts) || exists(StatisticsPageTags.Example) }
        compose.onNodeWithTag(StatisticsPageTags.Period).assertIsDisplayed()

        // The tab chip, not the hidden title the tab also keeps
        onView(allOf(withText("History"), isDisplayed())).perform(click())
        compose.waitUntil(LoadMillis) { exists(HistoryPageTags.Calendar) || exists(HistoryPageTags.Example) }
        compose.waitUntil(WaitMillis) { displayed(HistoryPageTags.View) }

        // The week's days open the statistics, though the history was the page shown last
        home.onActivity { it.findViewById<ViewPager>(R.id.pager).currentItem = WeekTab }
        compose.waitUntil(LoadMillis) { exists(WeekTabTags.Days) }
        compose.onNodeWithTag(WeekTabTags.Days).performClick()
        compose.waitUntil(WaitMillis) { home.currentTab() == ActivityTab && displayed(StatisticsPageTags.Period) }
    }

    private fun flagsOn() = RemoteConfigManager.manager.statisticsV2() && RemoteConfigManager.manager.historyV2()

    private fun ActivityScenario<HomeActivity>.currentTab(): Int {
        var tab = -1
        onActivity { tab = it.findViewById<ViewPager>(R.id.pager).currentItem }
        return tab
    }

    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    // On screen, rather than composed on a page the pager keeps off to the side
    private fun displayed(tag: String) = runCatching { compose.onNodeWithTag(tag).assertIsDisplayed() }.isSuccess

    private companion object {
        const val WeekTab = 0
        const val ActivityTab = 2
        const val LoadMillis = 30_000L
        const val WaitMillis = 5_000L
        const val PollMillis = 250L
    }
}
