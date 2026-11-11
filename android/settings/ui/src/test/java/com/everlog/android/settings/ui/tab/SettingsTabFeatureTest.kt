package com.everlog.android.settings.ui.tab

import android.content.Context
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import com.everlog.android.auth.data.model.ELUser
import com.everlog.android.auth.testfixtures.FakeUserSession
import com.everlog.android.common.composetest.FeatureTest
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.settings.data.MuscleGoal
import com.everlog.android.settings.testfixtures.FakeUserSettings
import com.everlog.android.settings.ui.SetSuggestionText
import com.everlog.android.settings.ui.SettingsAnalytics
import com.everlog.android.settings.ui.SettingsConfig
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.threeten.bp.DayOfWeek

/**
 * Home's Settings tab driven as a user would, with the real view-model over fakes: changing each kind of setting
 * through its sheet, the account and logging out, a guest, and the rows that open other screens. A few journeys rather
 * than a test per detail: the view-model's unit tests cover those.
 */
class SettingsTabFeatureTest : FeatureTest() {

    private val settings = FakeUserSettings(workoutsGoal = 3, weightStep = 1f, screenOn = true)
    private val session = FakeUserSession(ELUser(id = "user", displayName = "Alex Smith", email = "alex@example.com"))
    private val opened = mutableListOf<String>()
    private var changes = 0

    @Test
    fun changingSettingsThroughTheirSheets() {
        show()

        // A two-way choice: a tap picks and closes the sheet
        compose.onNodeWithText("First week day").performClick()
        compose.onNodeWithText("Sunday").performClick()
        waitUntil(WaitMillis) { exists("Sunday") && !exists("Monday") }
        assertThat(settings.firstDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY)

        compose.onNodeWithText("Weight unit").performClick()
        compose.onNodeWithText("Pounds (lb)").performClick()
        waitUntil(WaitMillis) { exists("Pounds (lb)") && !exists("Kilograms (kg)") }
        assertThat(settings.weightUnit()).isEqualTo(WeightUnit.POUND)

        // A wheel: picking moves it, Done saves
        compose.onNodeWithText("Weekly workouts goal").performClick()
        waitUntil(WaitMillis) { exists("5 workouts") }
        compose.onNodeWithText("5 workouts").performClick()
        waitUntil(WaitMillis) { settingsSheetGoal() == 5 }
        compose.onNodeWithText("Done").performClick()
        waitUntil(WaitMillis) { settings.weeklyWorkoutsGoal() == 5 }
        waitUntil(WaitMillis) { !exists("Done") }

        // Two wheels: 1 lb to 1.25 lb is one tap on the quarters
        scrollTo("Weight increment")
        compose.onNodeWithText("Weight increment").performClick()
        waitUntil(WaitMillis) { exists(".25") }
        compose.onNodeWithText(".25").performClick()
        waitUntil(WaitMillis) { settingsSheetQuarters() == 5 }
        compose.onNodeWithText("Done").performClick()
        waitUntil(WaitMillis) { settings.weightIncrease() == 1.25f }
        waitUntil(WaitMillis) { exists("1.25 lb") }

        scrollTo("Keep screen on during workouts")
        compose.onNodeWithTag(SettingsTabTags.KeepScreenOn).performClick()
        waitUntil(WaitMillis) { !settings.keepScreenOn() }

        // The first day, the unit and the goal tell the other screens
        assertThat(changes).isEqualTo(3)
    }

    @Test
    fun closingAWheelSheetKeepsTheValue() {
        show()

        compose.onNodeWithText("Weekly workouts goal").performClick()
        waitUntil(WaitMillis) { exists("5 workouts") }
        compose.onNodeWithText("5 workouts").performClick()
        waitUntil(WaitMillis) { settingsSheetGoal() == 5 }
        Espresso.pressBack()

        waitUntil(WaitMillis) { !exists("Done") }
        assertThat(settings.weeklyWorkoutsGoal()).isEqualTo(3)
        assertThat(changes).isEqualTo(0)
    }

    @Test
    fun theAccountAndLoggingOut() {
        show()

        compose.onNodeWithText("Alex").assertExists()
        scrollTo("Log out")
        compose.onNodeWithText("Log out").performClick()
        waitUntil(WaitMillis) { exists("Log out?") }

        // Staying closes the sheet
        compose.onNodeWithText("Stay logged in").performClick()
        waitUntil(WaitMillis) { !exists("Log out?") }
        assertThat(opened).isEmpty()

        compose.onNodeWithText("Log out").performClick()
        waitUntil(WaitMillis) { exists("Log out?") }
        compose.onNodeWithTag(SettingsTabTags.LogOutConfirm).performClick()
        waitUntil(WaitMillis) { "log out" in opened }
    }

    @Test
    fun aGuestHasNoAccountToLogOutOf() {
        session.updateUser(ELUser(id = "guest"))
        show()

        scrollTo("Share Everlog")
        compose.onNodeWithText("Alex").assertDoesNotExist()
        compose.onNodeWithText("Log out").assertDoesNotExist()
    }

    @Test
    fun rowsThatOpenOtherScreens() {
        show()

        compose.onNodeWithText("Set suggestions").performClick()
        scrollTo("Send feedback")
        compose.onNodeWithText("Send feedback").performClick()
        scrollTo("Review us on Google Play")
        compose.onNodeWithText("Review us on Google Play").performClick()
        scrollTo("Share Everlog")
        compose.onNodeWithText("Share Everlog").performClick()

        assertThat(opened).containsExactly("set suggestions", "feedback", "review", "share").inOrder()
    }

    private lateinit var viewModel: SettingsTabViewModel

    private fun show() {
        viewModel = SettingsTabViewModel(
            dispatcher = Dispatchers.Main,
            settings = settings,
            session = session,
            config = Config,
            analytics = Analytics,
            canSendFeedback = { true },
            showDeveloper = false,
            onSettingsChanged = { changes++ },
        )
        setContent {
            SettingsTabScreen(
                viewModel = viewModel,
                bottomPadding = 120.dp,
                actions = SettingsTabActions(
                    openSetSuggestions = { opened += "set suggestions" },
                    sendFeedback = { opened += "feedback" },
                    reviewOnPlay = { opened += "review" },
                    share = { opened += "share" },
                    openDesignSystem = { opened += "design system" },
                    logOut = { opened += "log out" },
                ),
                describeSetSuggestion = { SetSuggestionText("From last time", "Fill in the reps and weight you did last time") },
            )
        }
    }

    private fun settingsSheetGoal() = (viewModel.state.value.sheet as? SettingsTabSheet.WeeklyGoal)?.value

    private fun settingsSheetQuarters() = (viewModel.state.value.sheet as? SettingsTabSheet.WeightIncrease)?.quarters

    private fun scrollTo(text: String) {
        compose.onNodeWithTag(SettingsTabTags.List).performScrollToNode(hasText(text))
    }

    private fun exists(text: String) = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private object Config : SettingsConfig {
        override fun setSuggestionsV2() = true
        override fun setSuggestionText(context: Context, goal: MuscleGoal): SetSuggestionText? = null
        override fun appInfo() = "everlog 2.24.0"
    }

    private object Analytics : SettingsAnalytics {
        override fun weightIncreaseModified(value: Float) = Unit
        override fun weeklyGoalModified(value: Int) = Unit
        override fun keepScreenOnModified(value: Boolean) = Unit
        override fun weightUnitModified(unit: String) = Unit
        override fun firstWeekDayModified(day: String) = Unit
    }

    private companion object {
        const val WaitMillis = 5_000L
    }
}
