package com.everlog.data.controllers.workoutprefill

import com.everlog.data.controllers.statistics.ExerciseStatsController
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.preferences.SettingsManager
import com.everlog.managers.preferences.SettingsManager.MuscleGoal
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment
import com.everlog.utils.NumberUtils
import timber.log.Timber
import kotlin.math.ceil

class WorkoutPrefill1RMController: BaseWorkoutPrefillController() {

    override fun tag(): String {
        return "Prefill1RMController"
    }

    override fun prefill(stats: ExerciseStatsController.StatsResult, setToPrefill: ELSet, setIndex: Int) {
        // Make sure muscle goal is 1RM-based
        if (!SettingsManager.manager.muscleGoal().is1RMBased) {
            Timber.tag(tag()).d("Prefill not based on 1RM. Ignoring")
            return
        }
        // Compute 1RM goal for all eligible sets
        val targetWeight = calculate1RMPercentage(stats.orm, SettingsManager.manager.muscleGoal())
        if (!setToPrefill.isWeightEntered()) {
            printExerciseMessage(setIndex, String.format("1RM calculated: 1RM=%f, target=%f", stats.orm, targetWeight))
            setToPrefill.updateWeight(targetWeight)
        }
    }

    private fun calculate1RMPercentage(orm: Float, goal: MuscleGoal): Float {
        var targetWeight = goal.percent1RM() * orm
        if (!NumberUtils.isWhole(targetWeight)) {
            // Round up decimal points
            targetWeight = ceil(targetWeight.toDouble()).toFloat()
        }
        return targetWeight
    }
}