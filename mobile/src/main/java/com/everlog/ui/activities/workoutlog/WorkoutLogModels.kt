package com.everlog.ui.activities.workoutlog

import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.unfinishedSets
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.preferences.SettingsManager.WeightUnit

internal data class WorkoutLogState(
    val name: String,
    // The workout's created date, which its time counts from
    val startedAt: Long,
    val now: Long,
    val blocks: List<WorkoutBlock>,
    // The set being worked on: the next one not done after the last completed
    val current: SetRef?,
    // Weights show and are typed in this unit; the blocks hold kg
    val unit: WeightUnit,
    // The keypad's ± for a weight, in [unit]
    val weightStep: Float,
    val keepScreenOn: Boolean,
    val dialog: WorkoutLogDialog? = null,
    // Set once the workout is finished or discarded, for the screen to close
    val exit: WorkoutLogExit? = null,
) {
    val elapsedSeconds: Long
        get() = ((now - startedAt) / 1000).coerceAtLeast(0)

    val unfinishedSets: Int
        get() = blocks.unfinishedSets()
}

internal sealed interface WorkoutLogDialog {
    // Finish with sets not ticked
    data object Finish : WorkoutLogDialog

    // Leaving the workout. With sets done it offers to finish instead, saying how many unticked sets would be
    // saved as done.
    data class Discard(val setsCompleted: Int, val untickedSaved: Int) : WorkoutLogDialog
}

internal sealed interface WorkoutLogExit {
    data object Discarded : WorkoutLogExit

    // The workout as saved, for its summary
    data class Finished(val workout: ELWorkout) : WorkoutLogExit
}
