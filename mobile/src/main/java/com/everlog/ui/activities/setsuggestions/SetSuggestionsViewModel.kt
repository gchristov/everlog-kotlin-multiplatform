package com.everlog.ui.activities.setsuggestions

import com.everlog.ui.mvvm.CommonViewModel
import kotlinx.coroutines.CoroutineDispatcher

class SetSuggestionsViewModel(
    dispatcher: CoroutineDispatcher,
    private val settings: SetSuggestionsSettings,
    private val account: SetSuggestionsAccount,
    private val analytics: SetSuggestionsAnalytics,
) : CommonViewModel<SetSuggestionsState>(
    dispatcher = dispatcher,
    initialState = SetSuggestionsState(selected = SetSuggestion.of(settings.muscleGoal()), unit = settings.weightUnit()),
) {
    // A goal picked without Pro, applied once the user comes back having upgraded
    private var waitingForPro: SetSuggestion? = null

    // Also when coming back from the Pro screen
    fun onResume() {
        analytics.screenViewed()
        val waiting = waitingForPro ?: return
        waitingForPro = null
        if (account.isPro()) select(waiting)
    }

    /**
     * @return false when [option] needs Pro first, so the screen opens the Pro screen
     */
    fun onPick(option: SetSuggestion): Boolean {
        if (option == state.value.selected) return true
        if (option.goal.proLocked() && !account.isPro()) {
            waitingForPro = option
            return false
        }
        select(option)
        return true
    }

    fun onShowHow() {
        setState { copy(showingHow = true) }
        analytics.howOpened()
    }

    fun onHideHow() = setState { copy(showingHow = false) }

    private fun select(option: SetSuggestion) {
        settings.setMuscleGoal(option.goal)
        setState { copy(selected = option) }
    }
}
