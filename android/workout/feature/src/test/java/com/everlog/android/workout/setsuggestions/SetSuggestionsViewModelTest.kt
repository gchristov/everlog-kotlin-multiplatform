package com.everlog.android.workout.setsuggestions

import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.test.FakeCoroutineDispatcher
import com.everlog.android.settings.data.SettingsManager.MuscleGoal
import com.everlog.android.workout.setsuggestions.SetSuggestion
import com.everlog.android.workout.setsuggestions.SetSuggestionsAnalytics
import com.everlog.android.workout.setsuggestions.SetSuggestionsSettings
import com.everlog.android.workout.setsuggestions.SetSuggestionsViewModel
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SetSuggestionsViewModelTest {

    private val settings = FakeSetSuggestionsSettings()
    private val analytics = FakeSetSuggestionsAnalytics()

    private fun viewModel() = SetSuggestionsViewModel(FakeCoroutineDispatcher, settings, analytics)

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

        viewModel.onPick(SetSuggestion.Off)

        assertThat(viewModel.current.selected).isEqualTo(SetSuggestion.Off)
        assertThat(settings.changes).containsExactly(MuscleGoal.NONE)
    }

    @Test
    fun `picking a goal sets it`() {
        val viewModel = viewModel()

        viewModel.onPick(SetSuggestion.GetStronger)

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

    // Edge cases

    @Test
    fun `picking the option that's already set changes nothing`() {
        val viewModel = viewModel()

        viewModel.onPick(SetSuggestion.LastTime)

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
