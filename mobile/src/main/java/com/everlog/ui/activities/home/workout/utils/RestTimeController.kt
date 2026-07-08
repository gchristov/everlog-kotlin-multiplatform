package com.everlog.ui.activities.home.workout.utils

import android.content.Context
import android.widget.LinearLayout
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
                         navigator: Navigator?) : BaseWorkoutTimeController(mvpView, workout, binding.footerContentContainer) {

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
        val margin = context.resources.getDimensionPixelSize(R.dimen.activity_margin_half)
        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins(margin, margin, margin, 0)
        return WorkoutTimerView(context, R.layout.view_workout_timer_compact, layoutParams)
    }

    override fun timerViewIndex(): Int {
        // Sit above the add exercise button in footerContentContainer
        return 0
    }
}