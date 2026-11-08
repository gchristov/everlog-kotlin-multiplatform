package com.everlog.ui.activities.home

import android.app.Activity
import android.app.Instrumentation
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager.widget.ViewPager
import com.everlog.R
import com.everlog.android.settings.ui.tab.SettingsTabTags
import com.everlog.android.workout.setsuggestions.SetSuggestionsActivity
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
 * Home's Settings tab end to end, through the real Home screen: the Compose tab Remote Config's settings_tab_v2 picks
 * (on in the dev project), a setting saved through its sheet into the device's settings, and a row opening Set
 * suggestions. SettingsTabScenarioTest covers the tab itself. The weekly goal it changes is put back afterwards; it
 * doesn't log out, which would sign the E2E account out of the other tests.
 */
@RunWith(AndroidJUnit4::class)
class SettingsTabActivityTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(3, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val settings get() = ELApplication.graph.userSettings
    private var keptGoal = 0
    private var setSuggestions: Instrumentation.ActivityMonitor? = null

    @Before
    fun setUp() {
        E2EAccount.signIn()
        keptGoal = settings.weeklyWorkoutsGoal()
        settings.setWeeklyWorkoutsGoal(3)
        // A new install only has the defaults until its first fetch, and Home picks its tabs when it starts
        instrumentation.runOnMainSync { RemoteConfigManager.manager.refreshAppConfig() }
        val deadline = System.currentTimeMillis() + LoadMillis
        while (!RemoteConfigManager.manager.settingsTabV2() && System.currentTimeMillis() < deadline) Thread.sleep(PollMillis)
        assertThat(RemoteConfigManager.manager.settingsTabV2()).isTrue()
        setSuggestions = instrumentation.addMonitor(
            SetSuggestionsActivity::class.java.name,
            Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null),
            true,
        )
    }

    @After
    fun tearDown() {
        setSuggestions?.let(instrumentation::removeMonitor)
        settings.setWeeklyWorkoutsGoal(keptGoal)
    }

    @Test
    fun theSettingsTabSavesASettingAndOpensSetSuggestions() {
        val home = ActivityScenario.launch(HomeActivity::class.java)
        home.onActivity { it.findViewById<ViewPager>(R.id.pager).currentItem = SettingsTab }
        compose.waitUntil(LoadMillis) { exists("Weekly workouts goal") }

        // The weekly goal through its wheel sheet, saved for the whole app
        compose.onNodeWithText("Weekly workouts goal").performClick()
        compose.waitUntil(WaitMillis) { exists("5 workouts") }
        compose.onNodeWithText("5 workouts").performClick()
        // Done once the wheel has settled on 5
        compose.waitUntil(WaitMillis) {
            compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "5 workouts"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Done").performClick()
        compose.waitUntil(WaitMillis) { settings.weeklyWorkoutsGoal() == 5 }

        // A row that opens another screen
        compose.onNodeWithTag(SettingsTabTags.List).performScrollToNode(hasText("Set suggestions"))
        compose.onNodeWithText("Set suggestions").performClick()
        compose.waitUntil(WaitMillis) { setSuggestions!!.hits > 0 }
    }

    private fun exists(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val SettingsTab = 3
        const val LoadMillis = 30_000L
        const val WaitMillis = 5_000L
        const val PollMillis = 250L
    }
}
