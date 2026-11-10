package com.everlog.android.home.screen

import androidx.lifecycle.SavedStateHandle
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.mvvm.CommonViewModel
import com.everlog.android.home.HomeAnalytics
import com.everlog.android.plan.data.PlanProgress
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Home: which tab shows (kept through process death), the floating button each tab has and the sheets it opens,
 * the workout left going when Home comes back, and an app update waiting for a restart.
 */
class HomeViewModel(
    dispatcher: CoroutineDispatcher,
    private val savedState: SavedStateHandle,
    private val services: HomeServices,
    private val planProgress: PlanProgress,
    private val analytics: HomeAnalytics,
) : CommonViewModel<HomeState>(
    dispatcher,
    HomeState(tab = savedState.get<String>(TabKey)?.let(HomeTab::valueOf) ?: HomeTab.Week, planOngoing = planProgress.hasOngoingPlan()),
) {

    private val reopen = OngoingWorkoutReopen()

    init {
        services.onStarted()
    }

    fun onTabSelected(tab: HomeTab) {
        savedState[TabKey] = tab.name
        setState { copy(tab = tab) }
    }

    // Back goes to the Week tab first; false when it's already there and Home can close
    fun onBack(): Boolean {
        if (state.value.tab == HomeTab.Week) return false
        onTabSelected(HomeTab.Week)
        return true
    }

    fun onWeekEmpty(empty: Boolean) {
        setState { copy(weekEmpty = empty) }
    }

    // A plan started from another screen: Home shows it on the Week tab
    fun onPlanStarted() {
        onTabSelected(HomeTab.Week)
        setState { copy(planOngoing = true) }
    }

    fun onResumed(): HomeResume {
        services.onResumed()
        setState { copy(planOngoing = planProgress.hasOngoingPlan()) }
        val workout = services.ongoingWorkout()
        return when (reopen.onHomeShown(workout != null)) {
            OngoingWorkoutAction.Reopen -> {
                analytics.workoutResumedOnLaunch(workout!!.getCompletedSetsCount())
                HomeResume.Reopen(workout)
            }
            OngoingWorkoutAction.Prompt -> {
                // Still open from before the app went to the back
                if (state.value.prompt == null) {
                    analytics.ongoingWorkoutPromptShown(workout!!.getCompletedSetsCount())
                    setState { copy(prompt = HomePrompt.OngoingWorkout(workout)) }
                }
                HomeResume.Nothing
            }
            // Not with a workout going, as the restart would end it
            OngoingWorkoutAction.None -> HomeResume.CheckForUpdate
        }
    }

    fun onPaused() {
        services.onPaused()
    }

    // The workout to open again
    fun onResumeWorkout(): ELWorkout? {
        val prompt = state.value.prompt as? HomePrompt.OngoingWorkout ?: return null
        analytics.ongoingWorkoutPromptCancelled()
        setState { copy(prompt = null) }
        return prompt.workout
    }

    fun onDiscardWorkout() {
        services.discardOngoingWorkout()
        analytics.ongoingWorkoutDiscarded()
        setState { copy(prompt = null) }
    }

    // Closed with back: the workout stays for next time
    fun onPromptDismissed() {
        if (state.value.prompt == null) return
        analytics.ongoingWorkoutPromptCancelled()
        setState { copy(prompt = null) }
    }

    fun onButtonTapped() {
        when (state.value.button) {
            HomeButton.StartWorkout -> {
                analytics.startWorkoutTapped(fromEmptyWeek = false)
                setState { copy(sheet = HomeSheet.Start) }
            }
            HomeButton.New -> setState { copy(sheet = HomeSheet.New) }
            null -> Unit
        }
    }

    // The empty week's own button opens the same menu
    fun onStartFromEmptyWeek() {
        analytics.startWorkoutTapped(fromEmptyWeek = true)
        setState { copy(sheet = HomeSheet.Start) }
    }

    fun onSheetClosed() {
        setState { copy(sheet = null) }
    }

    fun onUpdateReady() {
        // Not with a workout going, as the restart would end it
        if (services.ongoingWorkout() != null) return
        setState { copy(updateReady = true) }
    }

    fun onUpdateShown() {
        setState { copy(updateReady = false) }
    }

    override fun onCleared() {
        services.onDestroyed()
        super.onCleared()
    }

    private companion object {
        const val TabKey = "tab"
    }
}
