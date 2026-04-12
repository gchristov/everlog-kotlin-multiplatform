package com.everlog.ui.activities.home.workout.utils

import android.content.Context
import com.ahamed.multiviewadapter.RecyclerAdapter
import com.everlog.data.model.exercise.ELRoutineExercise
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import com.everlog.databinding.ActivityWorkoutBinding
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.activities.home.workout.MvpViewWorkout
import com.everlog.ui.navigator.Navigator
import com.everlog.ui.views.revealcircle.WorkoutTimerView
import com.everlog.utils.Utils

class ExerciseTimeController(mvpView: MvpViewWorkout?,
                             workout: ELWorkout,
                             private val binding: ActivityWorkoutBinding,
                             workoutAdapter: RecyclerAdapter?,
                             navigator: Navigator?) : BaseWorkoutTimeController(mvpView, workout, binding.contentPanel) {

    private var mRunningExercise: ELRoutineExercise? = null
    private var mRunningSet: ELSet? = null
    private var mWorkoutAdapter = workoutAdapter
    private val mNavigator = navigator

    override fun timerStarted() {
        mRunningSet?.remainingTimeSeconds = mRunningSet?.getTimeSeconds()
        AnalyticsManager.manager.workoutTimerStartedExercise()
    }

    override fun timerStopped(userCancelled: Boolean) {
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

    override fun timerOffsetUpdated() {
        // Update time for this set
        mRunningSet?.updateTimeSeconds(mTotalTimeSeconds)
    }

    override fun timerTick(remainingSeconds: Int, remainingProgress: Int) {
        mRunningSet?.remainingTimeSeconds = remainingSeconds
        mNavigator?.notifyWorkoutServiceSetUpdated(mWorkout)
    }

    override fun getTimerTitle(): String {
        return mRunningExercise?.getName() ?: "Exercise timer"
    }

    override fun buildTimer(context: Context): WorkoutTimerView {
        return WorkoutTimerView(context)
    }

    fun setRunningData(exercise: ELRoutineExercise, set: ELSet) {
        this.mRunningExercise = exercise
        this.mRunningSet = set
    }
}