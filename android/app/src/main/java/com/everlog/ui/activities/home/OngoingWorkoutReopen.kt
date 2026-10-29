package com.everlog.ui.activities.home

internal enum class OngoingWorkoutAction {
    None,
    Reopen,
    Prompt,
}

// The home screen only shows with a workout ongoing when the app opened without the workout screen, e.g. after
// being swiped away mid-workout, so it goes straight back to it. Only once per workout: if the workout screen
// closes without ending it, reopening again could loop, so the user gets the prompt to resume or discard instead.
internal class OngoingWorkoutReopen {
    private var reopened = false

    fun onHomeShown(hasOngoingWorkout: Boolean): OngoingWorkoutAction {
        if (!hasOngoingWorkout) {
            reopened = false
            return OngoingWorkoutAction.None
        }
        if (reopened) return OngoingWorkoutAction.Prompt
        reopened = true
        return OngoingWorkoutAction.Reopen
    }
}
