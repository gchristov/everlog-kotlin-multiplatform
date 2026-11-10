package com.everlog.android.home.screen

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.home.HomeAnalytics

internal class FakeHomeServices : HomeServices {
    var ongoing: ELWorkout? = null
    var started = 0
    var resumed = 0
    var paused = 0
    var discarded = 0

    override fun ongoingWorkout() = ongoing

    override fun discardOngoingWorkout() {
        discarded++
        ongoing = null
    }

    override fun onStarted() {
        started++
    }

    override fun onResumed() {
        resumed++
    }

    override fun onPaused() {
        paused++
    }

    override fun onDestroyed() = Unit

    override fun appUpdates(context: Context, launcher: ActivityResultLauncher<IntentSenderRequest>, onReady: () -> Unit) =
        error("The activity asks for app updates, not the view-model or the screen")
}

internal class FakeHomeAnalytics : HomeAnalytics {
    val startTaps = mutableListOf<Boolean>()
    val resumedOnLaunch = mutableListOf<Int>()
    val promptsShown = mutableListOf<Int>()
    var promptsCancelled = 0
    var discarded = 0

    override fun planDayCompleted() = Unit

    override fun planCompleted() = Unit

    override fun notificationShown(title: String?) = Unit

    override fun notificationDismissed(title: String?) = Unit

    override fun notificationTapped(title: String?) = Unit

    override fun startWorkoutTapped(fromEmptyWeek: Boolean) {
        startTaps += fromEmptyWeek
    }

    override fun workoutResumedOnLaunch(setsCompleted: Int) {
        resumedOnLaunch += setsCompleted
    }

    override fun ongoingWorkoutPromptShown(setsCompleted: Int) {
        promptsShown += setsCompleted
    }

    override fun ongoingWorkoutPromptCancelled() {
        promptsCancelled++
    }

    override fun ongoingWorkoutDiscarded() {
        discarded++
    }

    override fun workoutsCardShown(kind: String) = Unit

    override fun workoutsTabStarted(from: String, card: String?, suggested: Boolean) = Unit
}
