package com.everlog.ui.activities.setsuggestions

import com.everlog.managers.preferences.SettingsManager.MuscleGoal
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.testutil.FakeCoroutineDispatcher
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SetSuggestionsViewModelTest {

    private val settings = FakeSetSuggestionsSettings()
    private val account = FakeSetSuggestionsAccount()
    private val analytics = FakeSetSuggestionsAnalytics()

    private fun viewModel() = SetSuggestionsViewModel(FakeCoroutineDispatcher, settings, account, analytics)

    private val SetSuggestionsViewModel.current get() = state.value

    // Main journeys

    @Test
    fun `opens on the option that's set`() {
        settings.goal = MuscleGoal.HISTORY

        assertThat(viewModel().current.selected).isEqualTo(SetSuggestion.LastTime)
    }

    @Test
    fun `picking an option sets it`() {
        val viewModel = viewModel()

        assertThat(viewModel.onPick(SetSuggestion.Off)).isTrue()

        assertThat(viewModel.current.selected).isEqualTo(SetSuggestion.Off)
        assertThat(settings.changes).containsExactly(MuscleGoal.NONE)
    }

    @Test
    fun `a Pro user picks a goal straight away`() {
        account.pro = true
        val viewModel = viewModel()

        assertThat(viewModel.onPick(SetSuggestion.GetStronger)).isTrue()

        assertThat(viewModel.current.selected).isEqualTo(SetSuggestion.GetStronger)
        assertThat(settings.changes).containsExactly(MuscleGoal.POWER)
    }

    @Test
    fun `the how sheet opens and closes`() {
        val viewModel = viewModel()

        viewModel.onShowHow()
        assertThat(viewModel.current.showingHow).isTrue()
        viewModel.onHideHow()

        assertThat(viewModel.current.showingHow).isFalse()
        assertThat(analytics.howOpened).isEqualTo(1)
    }

    @Test
    fun `logs a screen view on each resume`() {
        val viewModel = viewModel()

        viewModel.onResume()
        viewModel.onResume()

        assertThat(analytics.screenViews).isEqualTo(2)
    }

    // Pro

    @Test
    fun `a goal without Pro asks for Pro and changes nothing yet`() {
        val viewModel = viewModel()

        assertThat(viewModel.onPick(SetSuggestion.BuildMuscle)).isFalse()

        assertThat(viewModel.current.selected).isEqualTo(SetSuggestion.LastTime)
        assertThat(settings.changes).isEmpty()
    }

    @Test
    fun `a goal picked without Pro is set on coming back as Pro`() {
        val viewModel = viewModel()
        viewModel.onPick(SetSuggestion.BuildMuscle)

        account.pro = true
        viewModel.onResume()

        assertThat(viewModel.current.selected).isEqualTo(SetSuggestion.BuildMuscle)
        assertThat(settings.changes).containsExactly(MuscleGoal.GROWTH)
    }

    @Test
    fun `coming back without upgrading keeps the option and forgets the goal`() {
        val viewModel = viewModel()
        viewModel.onPick(SetSuggestion.BuildMuscle)

        viewModel.onResume()
        account.pro = true
        viewModel.onResume()

        assertThat(viewModel.current.selected).isEqualTo(SetSuggestion.LastTime)
        assertThat(settings.changes).isEmpty()
    }

    @Test
    fun `Off and last time never need Pro`() {
        settings.goal = MuscleGoal.GROWTH
        val viewModel = viewModel()

        assertThat(viewModel.onPick(SetSuggestion.LastTime)).isTrue()
        assertThat(viewModel.onPick(SetSuggestion.Off)).isTrue()

        assertThat(settings.changes).containsExactly(MuscleGoal.HISTORY, MuscleGoal.NONE).inOrder()
    }

    // Edge cases

    @Test
    fun `picking the option that's already set changes nothing`() {
        val viewModel = viewModel()

        assertThat(viewModel.onPick(SetSuggestion.LastTime)).isTrue()

        assertThat(settings.changes).isEmpty()
    }

    @Test
    fun `a goal the app no longer offers opens with nothing picked`() {
        settings.goal = MuscleGoal.ENDURANCE

        assertThat(viewModel().current.selected).isNull()
    }

    // The example on the how sheet

    @Test
    fun `the example is worked out in kg`() {
        settings.unit = WeightUnit.KILOGRAM

        val example = viewModel().current.example

        assertThat(listOf(example.weight, example.oneRepMax, example.goalWeight)).containsExactly(80f, 90f, 72f).inOrder()
        assertThat(example.reps).isEqualTo(5)
        assertThat(example.goal).isEqualTo(SetSuggestion.BuildMuscle)
    }

    @Test
    fun `the example is worked out in lb`() {
        settings.unit = WeightUnit.POUND

        val example = viewModel().current.example

        assertThat(listOf(example.weight, example.oneRepMax, example.goalWeight)).containsExactly(180f, 202.5f, 162f).inOrder()
    }
}

private class FakeSetSuggestionsSettings : SetSuggestionsSettings {
    var goal = MuscleGoal.HISTORY
    var unit = WeightUnit.KILOGRAM
    val changes = mutableListOf<MuscleGoal>()

    override fun muscleGoal() = goal

    override fun setMuscleGoal(goal: MuscleGoal) {
        this.goal = goal
        changes += goal
    }

    override fun weightUnit() = unit
}

private class FakeSetSuggestionsAccount : SetSuggestionsAccount {
    var pro = false

    override fun isPro() = pro
}

private class FakeSetSuggestionsAnalytics : SetSuggestionsAnalytics {
    var screenViews = 0
    var howOpened = 0

    override fun screenViewed() {
        screenViews++
    }

    override fun howOpened() {
        howOpened++
    }
}
