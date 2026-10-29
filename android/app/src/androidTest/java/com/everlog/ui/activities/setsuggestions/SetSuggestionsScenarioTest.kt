package com.everlog.ui.activities.setsuggestions

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.everlog.android.common.compose.theme.Theme
import com.everlog.managers.preferences.SettingsManager.MuscleGoal
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * The Set suggestions screen driven as a user would, with the real view-model over fake settings. The view-model's
 * unit tests cover the details.
 */
@RunWith(AndroidJUnit4::class)
class SetSuggestionsScenarioTest {

    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(Timeout.builder().withTimeout(2, TimeUnit.MINUTES).withLookingForStuckThread(true).build())
        .around(compose)

    private val settings = object : SetSuggestionsSettings {
        var goal = MuscleGoal.HISTORY
        var unit = WeightUnit.KILOGRAM

        override fun muscleGoal() = goal

        override fun setMuscleGoal(goal: MuscleGoal) {
            this.goal = goal
        }

        override fun weightUnit() = unit
    }

    @Test
    fun pickingAnOptionAndReadingHowGoalsWork() {
        show()

        // With no feedback form, there's no Send feedback
        compose.onNodeWithTag(SetSuggestionsTags.SendFeedback).assertDoesNotExist()

        // Off is a tap away
        compose.onNodeWithTag(SetSuggestionsTags.option(SetSuggestion.Off)).performClick()
        compose.waitUntil(WaitMillis) { settings.goal == MuscleGoal.NONE }

        // The sheet explains goal weights with the bench press example, in kg
        compose.onNodeWithTag(SetSuggestionsTags.How).performClick()
        compose.waitUntil(WaitMillis) { exists("80 kg × 5") }
        compose.onNodeWithText("90 kg").assertExists()
        compose.onNodeWithText("72 kg").assertExists()
        compose.onNodeWithText("Build muscle, 80%").assertExists()
        compose.onNodeWithTag(SetSuggestionsTags.HowDone).performClick()
        compose.waitUntil(WaitMillis) { !exists("80 kg × 5") }
    }

    @Test
    fun pickingAGoal() {
        show()

        compose.onNodeWithTag(SetSuggestionsTags.option(SetSuggestion.GetStronger)).performClick()

        compose.waitUntil(WaitMillis) { settings.goal == MuscleGoal.POWER }
    }

    @Test
    fun sendingFeedback() {
        var feedbackSent = 0
        show(onSendFeedback = { feedbackSent++ })

        compose.onNodeWithTag(SetSuggestionsTags.SendFeedback).performScrollTo().performClick()

        assertThat(feedbackSent).isEqualTo(1)
    }

    @Test
    fun theExampleFollowsTheWeightUnit() {
        settings.unit = WeightUnit.POUND
        show()

        compose.onNodeWithTag(SetSuggestionsTags.How).performClick()

        compose.waitUntil(WaitMillis) { exists("180 lb × 5") }
        compose.onNodeWithText("202.5 lb").assertExists()
        compose.onNodeWithText("162 lb").assertExists()
    }

    private fun show(onSendFeedback: (() -> Unit)? = null) {
        val viewModel = SetSuggestionsViewModel(
            dispatcher = Dispatchers.Main,
            settings = settings,
            analytics = object : SetSuggestionsAnalytics {
                override fun screenViewed() = Unit

                override fun howOpened() = Unit
            },
        )
        compose.setContent {
            Theme {
                SetSuggestionsScreen(viewModel = viewModel, onSendFeedback = onSendFeedback)
            }
        }
    }

    private fun exists(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val WaitMillis = 5_000L
    }
}
