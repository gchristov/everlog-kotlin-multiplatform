package com.everlog.ui.activities.workoutlog

import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.controllers.workoutsession.set
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
    // The highlighted set: the current one as the workout starts, then the last one touched. Once it's completed,
    // the next one.
    val selected: SetRef? = current,
    // The cell the keypad types into. The keypad is open while there is one.
    val focus: CellFocus? = null,
    // What's been typed into the focused cell. Null until the first key, which replaces its value.
    val typed: String? = null,
    // Goes up with every key, so the edited cell comes back into view if it was scrolled away
    val reveal: Int = 0,
    val dialog: WorkoutLogDialog? = null,
    // Set once the workout is finished or discarded, for the screen to close
    val exit: WorkoutLogExit? = null,
) {
    val elapsedSeconds: Long
        get() = ((now - startedAt) / 1000).coerceAtLeast(0)

    val unfinishedSets: Int
        get() = blocks.unfinishedSets()

    // What the open keypad's wide key does
    val mainKey: KeypadMainKey?
        get() {
            val focus = focus ?: return null
            val set = blocks.set(focus.ref) ?: return null
            return when {
                focus.field != ValueField.Weight -> KeypadMainKey.Next
                set.done -> KeypadMainKey.Done
                else -> KeypadMainKey.CompleteSet
            }
        }

    // The set the closed keypad's Complete set completes: the selected one, or else the current one
    val target: SetRef?
        get() = selected?.takeIf { blocks.set(it)?.done == false } ?: current
}

/**
 * A cell the keypad types into.
 */
internal data class CellFocus(val ref: SetRef, val field: ValueField)

// Reps or time, whichever the set records
internal fun countField(set: WorkoutSet) = if (set.mode == SetMode.Time) ValueField.Time else ValueField.Reps

// Reps (or time), then the weight, whose key completes the set. A done set's values save as they're typed, so its
// key just finishes editing.
internal enum class KeypadMainKey {
    Next,
    CompleteSet,
    Done,
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
