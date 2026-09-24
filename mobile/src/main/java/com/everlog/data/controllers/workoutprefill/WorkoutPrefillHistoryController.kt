package com.everlog.data.controllers.workoutprefill

import com.everlog.data.model.set.ELSet
import timber.log.Timber

class WorkoutPrefillHistoryController: BaseWorkoutPrefillController() {

    override fun tag(): String {
        return "PrefillHistoryController"
    }

    override fun prefill(source: PrefillSource, setToPrefill: ELSet, setIndex: Int) {
        // Make sure history is not empty
        val historicEntry = source.lastSession
        if (historicEntry == null) {
            Timber.tag(tag()).d("History empty. Ignoring")
            return
        }
        // Make sure we're looking at the correct set number, i.e. set 1 will prefill only from historic set 1, not set 2
        if (setIndex < historicEntry.exercise?.sets?.size ?: 0) {
            val historicSet = historicEntry.exercise?.sets?.get(setIndex)
            if (!setToPrefill.isWeightEntered() && historicSet?.isWeightEntered() == true) {
                // Prefill weight if current set doesn't have weight AND historic set has
                printExerciseMessage(historicEntry.workout!!, historicEntry.exercise!!, setIndex, String.format("Found historic weight value: %s", buildSetInfo(historicSet)))
                setToPrefill.updateWeight(historicSet.getWeight())
            }
        }
    }
}