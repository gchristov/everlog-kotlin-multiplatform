package com.everlog.android.workout.setsuggestions

import com.everlog.android.common.mvvm.CommonViewModel
import kotlinx.coroutines.CoroutineDispatcher

class SetSuggestionsViewModel(
    dispatcher: CoroutineDispatcher,
    private val settings: SetSuggestionsSettings,
    private val analytics: SetSuggestionsAnalytics,
) : CommonViewModel<SetSuggestionsState>(
    dispatcher = dispatcher,
    initialState = SetSuggestionsState(selected = SetSuggestion.of(settings.muscleGoal()), unit = settings.weightUnit()),
) {
    fun onResume() = analytics.screenViewed()

    fun onPick(option: SetSuggestion) {
        if (option == state.value.selected) return
        settings.setMuscleGoal(option.goal)
        setState { copy(selected = option) }
    }

    fun onShowHow() {
        setState { copy(showingHow = true) }
        analytics.howOpened()
    }

    fun onHideHow() = setState { copy(showingHow = false) }
}
