package com.everlog.ui.activities.home.workout.utils

import android.content.Context
import android.view.ViewGroup
import com.everlog.R
import com.everlog.data.model.workout.ELWorkout
import com.everlog.databinding.ActivityWorkoutBinding
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.activities.home.workout.MvpViewWorkout
import com.everlog.ui.navigator.Navigator
import com.everlog.ui.views.revealcircle.WorkoutTimerView

class RestTimeController(mvpView: MvpViewWorkout?,
                         workout: ELWorkout,
                         private val binding: ActivityWorkoutBinding,
                         navigator: Navigator?) : BaseWorkoutTimeController(mvpView, workout, binding.contentPanel) {

    private val mNavigator = navigator

    override fun timerStarted() {
        // Change notification to be the rest timer
        mNavigator?.notifyWorkoutServiceShowRestTimer(100, mTotalTimeSeconds)
        AnalyticsManager.manager.workoutTimerStartedRest()
    }

    override fun timerStopped(userCancelled: Boolean) {
        // Hide rest timer
        mNavigator?.notifyWorkoutServiceHideRestTimer(mWorkout)
        if (userCancelled) {
            AnalyticsManager.manager.workoutTimerStoppedRest()
        }
    }

    override fun timerOffsetUpdated() {
        // Update rest time for all sets
        mWorkout.updateRestTime(mTotalTimeSeconds)
    }

    override fun timerTick(remainingSeconds: Int, remainingProgress: Int) {
        mNavigator?.notifyWorkoutServiceShowRestTimer(remainingProgress, remainingSeconds)
    }

    override fun getTimerTitle(): String {
        return "Rest"
    }

    override fun buildTimer(context: Context): WorkoutTimerView {
        return WorkoutTimerView(context, R.layout.view_workout_timer_compact, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}