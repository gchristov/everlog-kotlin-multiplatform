package com.everlog.ui.activities.setsuggestions

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.everlog.android.settings.data.SettingsManager
import com.everlog.android.settings.data.SettingsManager.MuscleGoal
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * The Set suggestions screen end to end, through the real activity and the setting it writes.
 * SetSuggestionsScenarioTest covers the screen itself.
 */
@RunWith(AndroidJUnit4::class)
class SetSuggestionsActivityTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(2, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var goalBefore: MuscleGoal

    @Before
    fun setUp() {
        goalBefore = SettingsManager.manager.muscleGoal()
        SettingsManager.manager.setMuscleGoal(MuscleGoal.HISTORY)
    }

    @After
    fun tearDown() {
        SettingsManager.manager.setMuscleGoal(goalBefore)
    }

    @Test
    fun changingTheSetting() {
        ActivityScenario.launch<SetSuggestionsActivity>(SetSuggestionsActivity.launchIntent(instrumentation.targetContext)).use {
            compose.onNodeWithTag(SetSuggestionsTags.option(SetSuggestion.Off)).performClick()
            compose.waitUntil(WaitMillis) { SettingsManager.manager.muscleGoal() == MuscleGoal.NONE }

            compose.onNodeWithTag(SetSuggestionsTags.option(SetSuggestion.BuildMuscle)).performClick()
            compose.waitUntil(WaitMillis) { SettingsManager.manager.muscleGoal() == MuscleGoal.GROWTH }
        }
    }

    private companion object {
        const val WaitMillis = 5_000L
    }
}
