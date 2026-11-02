package com.everlog.android.workout.data.prefill

import com.everlog.android.common.data.model.set.ELSet
import timber.log.Timber

class WorkoutPrefill1RMController: BaseWorkoutPrefillController() {

    override fun tag(): String {
        return "Prefill1RMController"
    }

    override fun prefill(source: PrefillSource, setToPrefill: ELSet, setIndex: Int, settings: PrefillSettings) {
        // Make sure muscle goal is 1RM-based
        if (!settings.muscleGoal.is1RMBased) {
            Timber.tag(tag()).d("Prefill not based on 1RM. Ignoring")
            return
        }
        // Make sure there's a 1RM to target
        if (source.orm <= 0) {
            Timber.tag(tag()).d("No 1RM found. Ignoring")
            return
        }
        // Compute 1RM goal for all eligible sets
        val targetWeight = goalWeight(source.orm, settings.muscleGoal)
        if (!setToPrefill.isWeightEntered()) {
            printExerciseMessage(setIndex, String.format("1RM calculated: 1RM=%f, target=%f", source.orm, targetWeight))
            setToPrefill.updateWeight(targetWeight, settings.weightUnit)
        }
    }
}