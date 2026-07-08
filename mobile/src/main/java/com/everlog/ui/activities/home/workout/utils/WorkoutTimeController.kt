package com.everlog.ui.activities.home.workout.utils

import android.content.Context
import android.widget.LinearLayout
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.R
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import com.everlog.databinding.ActivityWorkoutBinding
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.activities.home.workout.MvpViewWorkout
import com.everlog.ui.navigator.Navigator
import com.everlog.ui.views.revealcircle.WorkoutTimerView
import com.everlog.utils.Utils

class WorkoutTimeController(mvpView: MvpViewWorkout?,
                            workout: ELWorkout,
                            private val binding: ActivityWorkoutBinding,
                            workoutAdapter: RecyclerAdapter?,
                            navigator: Navigator?) : BaseWorkoutTimeController(mvpView, workout, binding.footerContentContainer) {

    private var mWorkoutAdapter = workoutAdapter
    private val mNavigator = navigator

    // A single timer can be used for either a rest or an exercise countdown, never both at once.
    private var mIsRest = false
    private var mTitle = ""
    private var mRunningSet: ELSet? = null

    fun startRestTimer(timeSeconds: Int) {
        mIsRest = true
        mTitle = "Rest"
        mRunningSet = null
        startTimer(timeSeconds)
    }

    fun startExerciseTimer(exercise: ELRoutineExercise, set: ELSet) {
        mIsRest = false
        mTitle = exercise.getName() ?: "Exercise timer"
        mRunningSet = set
        startTimer(set.getTimeSeconds())
    }

    fun isRunningExerciseTimer(): Boolean {
        return isActive() && !mIsRest
    }

    override fun timerStarted() {
        if (mIsRest) {
            // Change notification to be the rest timer
            mNavigator?.notifyWorkoutServiceShowRestTimer(100, mTotalTimeSeconds)
            AnalyticsManager.manager.workoutTimerStartedRest()
        } else {
            mRunningSet?.remainingTimeSeconds = mRunningSet?.getTimeSeconds()
            AnalyticsManager.manager.workoutTimerStartedExercise()
        }
    }

    override fun timerStopped(userCancelled: Boolean) {
        if (mIsRest) {
            // Hide rest timer
            mNavigator?.notifyWorkoutServiceHideRestTimer(mWorkout)
            if (userCancelled) {
                AnalyticsManager.manager.workoutTimerStoppedRest()
            }
        } else {
            mRunningSet?.remainingTimeSeconds = null
            mRunningSet = null
            mNavigator?.notifyWorkoutServiceSetUpdated(mWorkout)
            if (userCancelled) {
                AnalyticsManager.manager.workoutTimerStoppedExercise()
            }
            Utils.runWithDelay({
                mWorkoutAdapter?.notifyDataSetChanged()
            }, 300)
        }
    }

    override fun timerOffsetUpdated() {
        if (mIsRest) {
            // Update rest time for all sets
            mWorkout.updateRestTime(mTotalTimeSeconds)
        } else {
            // Update time for this set
            mRunningSet?.updateTimeSeconds(mTotalTimeSeconds)
        }
    }

    override fun timerTick(remainingSeconds: Int, remainingProgress: Int) {
        if (mIsRest) {
            mNavigator?.notifyWorkoutServiceShowRestTimer(remainingProgress, remainingSeconds)
        } else {
            mRunningSet?.remainingTimeSeconds = remainingSeconds
            mNavigator?.notifyWorkoutServiceSetUpdated(mWorkout)
        }
    }

    override fun getTimerTitle(): String {
        return mTitle
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
