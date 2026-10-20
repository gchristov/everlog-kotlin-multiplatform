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
    // The one countdown running, a rest or a timed set's. Not saved: like the old screen's, it's gone if the app is
    // killed.
    val timer: WorkoutTimer? = null,
    // Timed sets whose timer has run out or been stopped, which are then ticked rather than timed again
    val timedSets: Set<SetRef> = emptySet(),
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
                startsTimer(focus.ref) -> KeypadMainKey.StartTimer
                else -> KeypadMainKey.CompleteSet
            }
        }

    // The set the closed keypad's main action completes (or times): the selected one, or else the current one
    val target: SetRef?
        get() = selected?.takeIf { blocks.set(it)?.done == false } ?: current

    // The closed keypad's main action for [target]: a timed set's timer starts first
    val targetStartsTimer: Boolean
        get() = target?.let(::startsTimer) == true

    // A timed set not done whose timer hasn't run yet: its main action starts the timer, and its row shows a timer
    // button in place of the tick. Once the timer has run, it's ticked like any set, as on the old screen.
    fun startsTimer(ref: SetRef): Boolean {
        val set = blocks.set(ref) ?: return false
        return set.mode == SetMode.Time && !set.done && !isTiming(ref) && ref !in timedSets
    }

    fun isTiming(ref: SetRef): Boolean = (timer as? WorkoutTimer.SetTimer)?.ref == ref

    // Whole seconds left on the countdown, rounded up so it shows 1 until it's done
    fun timerRemainingSeconds(): Int = timer?.let { ((it.endsAt - now).coerceAtLeast(0) + 999).toInt() / 1000 } ?: 0
}

/**
 * A countdown that ends at [endsAt]. [totalSeconds] is how long it runs, with any time added or taken off.
 */
internal sealed interface WorkoutTimer {
    val endsAt: Long
    val totalSeconds: Int

    // After a set, before the next
    data class Rest(override val endsAt: Long, override val totalSeconds: Int) : WorkoutTimer

    // A timed set being done
    data class SetTimer(val ref: SetRef, override val endsAt: Long, override val totalSeconds: Int) : WorkoutTimer
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
    // A timed set's timer, before Complete set
    StartTimer,
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
