package com.everlog.ui.activities.setsuggestions

import android.app.Instrumentation
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.managers.auth.LocalUserManager
import com.everlog.managers.preferences.SettingsManager
import com.everlog.managers.preferences.SettingsManager.MuscleGoal
import com.everlog.testutil.E2EAccount
import com.everlog.ui.activities.home.pro.ProActivity
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
 * The Set suggestions screen end to end, through the real activity: the setting it writes, and the Pro screen a goal
 * opens without Pro (intercepted). SetSuggestionsScenarioTest covers the screen itself.
 */
@RunWith(AndroidJUnit4::class)
class SetSuggestionsActivityTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(2, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var proScreen: Instrumentation.ActivityMonitor? = null
    private lateinit var goalBefore: MuscleGoal

    @Before
    fun setUp() {
        E2EAccount.signIn()
        goalBefore = SettingsManager.manager.muscleGoal()
        SettingsManager.manager.setMuscleGoal(MuscleGoal.HISTORY)
        proScreen = instrumentation.addMonitor(ProActivity::class.java.name, null, true)
    }

    @After
    fun tearDown() {
        proScreen?.let(instrumentation::removeMonitor)
        SettingsManager.manager.setMuscleGoal(goalBefore)
    }

    @Test
    fun changingTheSetting() {
        ActivityScenario.launch<SetSuggestionsActivity>(SetSuggestionsActivity.launchIntent(instrumentation.targetContext)).use {
            compose.onNodeWithTag(SetSuggestionsTags.option(SetSuggestion.Off)).performClick()
            compose.waitUntil(WaitMillis) { SettingsManager.manager.muscleGoal() == MuscleGoal.NONE }

            compose.onNodeWithTag(SetSuggestionsTags.option(SetSuggestion.BuildMuscle)).performClick()
            if (LocalUserManager.getUser()?.isPro() == true) {
                compose.waitUntil(WaitMillis) { SettingsManager.manager.muscleGoal() == MuscleGoal.GROWTH }
            } else {
                compose.waitUntil(WaitMillis) { proScreen!!.hits > 0 }
                assertThat(SettingsManager.manager.muscleGoal()).isEqualTo(MuscleGoal.NONE)
            }
        }
    }

    private companion object {
        const val WaitMillis = 5_000L
    }
}
