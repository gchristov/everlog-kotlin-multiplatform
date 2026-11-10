package com.everlog.android.home.screen

import com.everlog.android.common.data.model.workout.ELWorkout

// Home's tabs, in the bottom bar's order
enum class HomeTab { Week, Workouts, Activity, Settings }

data class HomeState(
    val tab: HomeTab,
    // The week shows a button of its own while it's empty; assumed empty until it loads, so Home's doesn't flash
    val weekEmpty: Boolean = true,
    val planOngoing: Boolean = false,
    val sheet: HomeSheet? = null,
    val prompt: HomePrompt? = null,
    // An app update downloaded, waiting for a restart
    val updateReady: Boolean = false,
) {
    // The floating button the tab shows: the Week tab leaves starting to its own buttons while it has them
    val button: HomeButton?
        get() = when (tab) {
            HomeTab.Week -> HomeButton.StartWorkout.takeUnless { weekEmpty || planOngoing }
            HomeTab.Workouts -> HomeButton.New
            HomeTab.Activity, HomeTab.Settings -> HomeButton.StartWorkout
        }
}

enum class HomeButton { StartWorkout, New }

enum class HomeSheet {
    // From a template, or empty
    Start,

    // A new template or plan
    New,
}

sealed interface HomePrompt {
    // A workout was left going: resume or discard it
    data class OngoingWorkout(val workout: ELWorkout) : HomePrompt
}

// What Home opens when it comes back to the front
sealed interface HomeResume {
    data class Reopen(val workout: ELWorkout) : HomeResume

    // Nothing in the way, so an app update can be offered
    data object CheckForUpdate : HomeResume

    data object Nothing : HomeResume
}
