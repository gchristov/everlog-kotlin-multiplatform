package com.everlog.ui.activities.home.workout.utils

import androidx.appcompat.app.AppCompatActivity
import com.everlog.data.model.workout.ELWorkout
import com.everlog.databinding.ActivityWorkoutBinding
import com.everlog.managers.preferences.SettingsManager
import com.everlog.ui.activities.home.workout.MvpViewWorkout
import com.everlog.utils.text.TextViewUtils

class WorkoutSetController(workout: ELWorkout, mvpView: MvpViewWorkout?, private val binding: ActivityWorkoutBinding) {

    private var mWorkout = workout
    private var mMvpView = mvpView
    private var mGoal: SettingsManager.MuscleGoal? = null

    fun workoutTimerTick() {
        (mMvpView?.getActivity() as? AppCompatActivity)?.supportActionBar?.title = mWorkout.getOngoingDuration()
        renderMuscleGoal()
    }

    private fun renderMuscleGoal() {
        val goal = SettingsManager.manager.muscleGoal()
        if (mGoal != goal) {
            binding.muscleGoalLbl.text = goal.valueSettingsSummary(mMvpView?.getActivity(), true)
            binding.muscleGoalManageLbl.text = if (goal.muscleGoalSet()) "Manage" else "Set a Goal"
            val linkText: String = binding.muscleGoalManageLbl.text.toString()
            TextViewUtils.addWorkoutLinkSpan(binding.muscleGoalManageLbl, linkText, 0, linkText.length)
        }
    }
}