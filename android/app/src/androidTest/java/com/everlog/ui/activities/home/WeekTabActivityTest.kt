package com.everlog.ui.activities.home

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager.widget.ViewPager
import com.everlog.R
import com.everlog.android.home.weektab.WeekTabTags
import com.everlog.android.plan.data.model.ELPlan
import com.everlog.android.plan.data.model.ELPlanState
import com.everlog.application.ELApplication
import com.everlog.managers.RemoteConfigManager
import com.everlog.testutil.E2EAccount
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * Home's Week tab end to end, through the real Home screen: the Compose tab Remote Config's week_tab_v2 picks (on in
 * the dev project), its week read through DataSync for the E2E account, and its taps switching Home's tabs.
 * WeekTabScenarioTest covers the tab itself. It reads the account and saves nothing; the device's ongoing plan, if
 * any, is put aside for the test and back afterwards.
 */
@RunWith(AndroidJUnit4::class)
class WeekTabActivityTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(3, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val planProgress get() = ELApplication.graph.planProgress
    private var keptPlan: ELPlan? = null
    private var keptState: ELPlanState? = null

    @Before
    fun setUp() {
        E2EAccount.signIn()
        keptPlan = planProgress.ongoingPlan()
        keptState = planProgress.ongoingPlanState()
        planProgress.clearOngoingPlan()
        // A new install only has the defaults until its first fetch, and Home picks its tab when it starts
        instrumentation.runOnMainSync { RemoteConfigManager.manager.refreshAppConfig() }
        val deadline = System.currentTimeMillis() + LoadMillis
        while (!RemoteConfigManager.manager.weekTabV2() && System.currentTimeMillis() < deadline) Thread.sleep(PollMillis)
        assertThat(RemoteConfigManager.manager.weekTabV2()).isTrue()
    }

    @After
    fun tearDown() {
        planProgress.clearOngoingPlan()
        keptPlan?.let { plan ->
            planProgress.setOngoingPlan(plan)
            keptState?.let(planProgress::setOngoingState)
        }
    }

    @Test
    fun theWeekTabLoadsAndOpensHomesOtherTabs() {
        val home = ActivityScenario.launch(HomeActivity::class.java)

        // The week has loaded once its numbers show
        compose.waitUntil(LoadMillis) { exists("Statistics") }

        compose.onNodeWithTag(WeekTabTags.Goal).performClick()
        compose.waitUntil(WaitMillis) { home.currentTab() == SettingsTab }

        home.onActivity { it.findViewById<ViewPager>(R.id.pager).currentItem = WeekTab }
        compose.onNodeWithTag(WeekTabTags.Days).performClick()
        compose.waitUntil(WaitMillis) { home.currentTab() == ActivityTab }
    }

    private fun ActivityScenario<HomeActivity>.currentTab(): Int {
        var tab = -1
        onActivity { tab = it.findViewById<ViewPager>(R.id.pager).currentItem }
        return tab
    }

    private fun exists(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val WeekTab = 0
        const val ActivityTab = 2
        const val SettingsTab = 3
        const val LoadMillis = 30_000L
        const val WaitMillis = 5_000L
        const val PollMillis = 250L
    }
}
