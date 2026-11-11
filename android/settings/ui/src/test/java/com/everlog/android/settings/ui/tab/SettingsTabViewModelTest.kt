package com.everlog.android.settings.ui.tab

import android.content.Context
import com.everlog.android.auth.data.model.ELUser
import com.everlog.android.auth.testfixtures.FakeUserSession
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.settings.data.MuscleGoal
import com.everlog.android.settings.testfixtures.FakeUserSettings
import com.everlog.android.settings.ui.SetSuggestionText
import com.everlog.android.settings.ui.SettingsAnalytics
import com.everlog.android.settings.ui.SettingsConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.threeten.bp.DayOfWeek

class SettingsTabViewModelTest {

    private val settings = FakeUserSettings(workoutsGoal = 3, weightStep = 2.5f, screenOn = true)
    private val session = FakeUserSession(ELUser(id = "user", displayName = "Alex Smith", email = "alex@example.com", photoUrl = "https://example.com/alex.jpg"))
    private val config = FakeSettingsConfig()
    private val analytics = FakeSettingsAnalytics()
    private var feedback = true
    private var changes = 0

    private fun viewModel(showDeveloper: Boolean = false) = SettingsTabViewModel(
        dispatcher = FakeCoroutineDispatcher,
        settings = settings,
        session = session,
        config = config,
        analytics = analytics,
        canSendFeedback = { feedback },
        showDeveloper = showDeveloper,
        onSettingsChanged = { changes++ },
    )

    // Showing

    @Test
    fun `shows the signed-in account and the settings`() {
        val state = viewModel().state.value

        assertThat(state.account).isEqualTo(SettingsAccountInfo(name = "Alex", photoUrl = "https://example.com/alex.jpg"))
        assertThat(state.firstDayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(state.weightUnit).isEqualTo(WeightUnit.KILOGRAM)
        assertThat(state.weeklyGoal).isEqualTo(3)
        assertThat(state.weightIncrease).isEqualTo(2.5f)
        assertThat(state.keepScreenOn).isTrue()
        assertThat(state.appInfo).isEqualTo("everlog 2.24.0")
        assertThat(state.sheet).isNull()
    }

    @Test
    fun `a guest has no account to show or log out of`() {
        session.updateUser(ELUser(id = "guest"))

        assertThat(viewModel().state.value.account).isNull()
    }

    @Test
    fun `the developer section and feedback follow what the tab is given`() {
        feedback = false

        val state = viewModel(showDeveloper = true).state.value

        assertThat(state.showDeveloper).isTrue()
        assertThat(state.showFeedback).isFalse()
    }

    @Test
    fun `refreshing picks up a goal changed on Set suggestions and Remote Config's feedback form`() {
        val viewModel = viewModel()
        feedback = false

        settings.setMuscleGoal(MuscleGoal.STRENGTH)
        viewModel.refresh()

        val state = viewModel.state.value
        assertThat(state.setSuggestion).isEqualTo(MuscleGoal.STRENGTH)
        assertThat(state.showFeedback).isFalse()
    }

    @Test
    fun `refreshing keeps an open sheet`() {
        val viewModel = viewModel()
        viewModel.onWeeklyGoalTapped()

        viewModel.refresh()

        assertThat(viewModel.state.value.sheet).isEqualTo(SettingsTabSheet.WeeklyGoal(3))
    }

    // Choices

    @Test
    fun `picking the first day of the week saves it and tells the other screens`() {
        val viewModel = viewModel()
        viewModel.onFirstDayOfWeekTapped()
        assertThat(viewModel.state.value.sheet).isEqualTo(SettingsTabSheet.FirstDayOfWeek)

        viewModel.onFirstDayOfWeekPicked(DayOfWeek.SUNDAY)

        assertThat(settings.firstDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY)
        assertThat(viewModel.state.value.firstDayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
        assertThat(viewModel.state.value.sheet).isNull()
        assertThat(changes).isEqualTo(1)
        assertThat(analytics.events).containsExactly("firstWeekDay SUNDAY")
    }

    @Test
    fun `picking the weight unit saves it and tells the other screens`() {
        val viewModel = viewModel()
        viewModel.onWeightUnitTapped()

        viewModel.onWeightUnitPicked(WeightUnit.POUND)

        assertThat(settings.weightUnit()).isEqualTo(WeightUnit.POUND)
        assertThat(viewModel.state.value.weightUnit).isEqualTo(WeightUnit.POUND)
        assertThat(viewModel.state.value.sheet).isNull()
        assertThat(changes).isEqualTo(1)
        assertThat(analytics.events).containsExactly("weightUnit POUND")
    }

    // Weekly goal

    @Test
    fun `the weekly goal wheel saves only on Done`() {
        val viewModel = viewModel()
        viewModel.onWeeklyGoalTapped()

        viewModel.onWeeklyGoalPicked(7)
        assertThat(viewModel.state.value.sheet).isEqualTo(SettingsTabSheet.WeeklyGoal(7))
        assertThat(settings.weeklyWorkoutsGoal()).isEqualTo(3)

        viewModel.onWeeklyGoalSaved(7)

        assertThat(settings.weeklyWorkoutsGoal()).isEqualTo(7)
        assertThat(viewModel.state.value.weeklyGoal).isEqualTo(7)
        assertThat(viewModel.state.value.sheet).isNull()
        assertThat(changes).isEqualTo(1)
        assertThat(analytics.events).containsExactly("weeklyGoal 7")
    }

    @Test
    fun `closing the weekly goal sheet keeps the goal`() {
        val viewModel = viewModel()
        viewModel.onWeeklyGoalTapped()
        viewModel.onWeeklyGoalPicked(5)

        viewModel.onSheetDismissed()

        assertThat(settings.weeklyWorkoutsGoal()).isEqualTo(3)
        assertThat(viewModel.state.value.sheet).isNull()
        assertThat(changes).isEqualTo(0)
        assertThat(analytics.events).isEmpty()
    }

    @Test
    fun `a goal saved outside 1 to 7 opens the wheel within it`() {
        settings.setWeeklyWorkoutsGoal(9)
        val viewModel = viewModel()

        viewModel.onWeeklyGoalTapped()

        assertThat(viewModel.state.value.sheet).isEqualTo(SettingsTabSheet.WeeklyGoal(7))
    }

    @Test
    fun `picking on a wheel without its sheet open does nothing`() {
        val viewModel = viewModel()

        viewModel.onWeeklyGoalPicked(5)
        viewModel.onWeightIncreasePicked(3, 1)

        assertThat(viewModel.state.value.sheet).isNull()
    }

    // Weight increase

    @Test
    fun `the weight increase opens on its whole and quarter`() {
        val viewModel = viewModel()

        viewModel.onWeightIncreaseTapped()

        val sheet = viewModel.state.value.sheet as SettingsTabSheet.WeightIncrease
        assertThat(sheet.whole).isEqualTo(2)
        assertThat(sheet.quarter).isEqualTo(2)
    }

    @Test
    fun `the two wheels pick 1 25 and Done saves it`() {
        val viewModel = viewModel()
        viewModel.onWeightIncreaseTapped()

        viewModel.onWeightIncreasePicked(whole = 1, quarter = 2)
        viewModel.onWeightIncreasePicked(whole = 1, quarter = 1)
        val sheet = viewModel.state.value.sheet as SettingsTabSheet.WeightIncrease
        viewModel.onWeightIncreaseSaved(sheet.quarters)

        assertThat(settings.weightIncrease()).isEqualTo(1.25f)
        assertThat(viewModel.state.value.weightIncrease).isEqualTo(1.25f)
        assertThat(viewModel.state.value.sheet).isNull()
        // Only the workout screen reads it, when a workout starts
        assertThat(changes).isEqualTo(0)
        assertThat(analytics.events).containsExactly("weightIncrease 1.25")
    }

    @Test
    fun `the weight increase stays within 1 and 20 75`() {
        val viewModel = viewModel()
        viewModel.onWeightIncreaseTapped()

        viewModel.onWeightIncreasePicked(whole = 20, quarter = 3)
        assertThat((viewModel.state.value.sheet as SettingsTabSheet.WeightIncrease).quarters).isEqualTo(83)

        viewModel.onWeightIncreasePicked(whole = 0, quarter = 0)
        assertThat((viewModel.state.value.sheet as SettingsTabSheet.WeightIncrease).quarters).isEqualTo(4)
    }

    @Test
    fun `a weight increase saved off the quarters opens on the nearest one`() {
        assertThat(weightIncreaseQuarters(2.3f)).isEqualTo(9)
        assertThat(weightIncreaseQuarters(0.5f)).isEqualTo(4)
        assertThat(weightIncreaseQuarters(25f)).isEqualTo(83)
    }

    // Keep screen on

    @Test
    fun `tapping keep screen on switches it`() {
        val viewModel = viewModel()

        viewModel.onKeepScreenOnTapped()

        assertThat(settings.keepScreenOn()).isFalse()
        assertThat(viewModel.state.value.keepScreenOn).isFalse()
        assertThat(analytics.events).containsExactly("keepScreenOn false")
    }

    // Log out

    @Test
    fun `log out asks first`() {
        val viewModel = viewModel()

        viewModel.onLogOutTapped()

        assertThat(viewModel.state.value.sheet).isEqualTo(SettingsTabSheet.Logout)
    }
}

private class FakeSettingsConfig : SettingsConfig {
    override fun setSuggestionText(context: Context, goal: MuscleGoal): SetSuggestionText? = null
    override fun appInfo() = "everlog 2.24.0"
}

private class FakeSettingsAnalytics : SettingsAnalytics {
    val events = mutableListOf<String>()
    override fun weightIncreaseModified(value: Float) { events += "weightIncrease $value" }
    override fun weeklyGoalModified(value: Int) { events += "weeklyGoal $value" }
    override fun keepScreenOnModified(value: Boolean) { events += "keepScreenOn $value" }
    override fun weightUnitModified(unit: String) { events += "weightUnit $unit" }
    override fun firstWeekDayModified(day: String) { events += "firstWeekDay $day" }
}
