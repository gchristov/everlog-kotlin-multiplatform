package com.everlog.ui.activities.workoutlog

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.settings.data.SettingsManager.MuscleGoal
import com.everlog.data.controllers.workoutsession.DeletedSets
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.controllers.workoutsession.set
import com.everlog.data.controllers.workoutsession.unfinishedSets
import com.everlog.ui.components.exercisetable.CellFocus

internal data class WorkoutLogState(
    val name: String,
    val startedAt: Long,
    val now: Long,
    val blocks: List<WorkoutBlock>,
    // The set being worked on: the next one not done after the last completed
    val current: SetRef?,
    // Weights show and are typed in this unit; the blocks hold kg
    val unit: WeightUnit,
    val weightStep: Float,
    val keepScreenOn: Boolean,
    // What fills sets in, as the sets were last filled
    val suggestions: MuscleGoal,
    // The highlighted set: the current one as the workout starts, then the last one touched. Once it's completed,
    // the next one.
    val selected: SetRef? = current,
    // The keypad is open while a cell has focus
    val focus: CellFocus? = null,
    // What's been typed into the focused cell. Null until the first key, which replaces its value.
    val typed: String? = null,
    // Goes up with every key, so the edited cell comes back into view if it was scrolled away
    val reveal: Int = 0,
    // Not saved, so it's gone if the app is killed
    val timer: WorkoutTimer? = null,
    // Timed sets whose timer has run out or been stopped, which are completed next rather than timed again
    val timedSets: Set<SetRef> = emptySet(),
    // Undo puts it back until anything else changes
    val deletion: Deletion? = null,
    // Shown once, then cleared with onMessageShown
    val message: WorkoutLogMessage? = null,
    // Each exercise's last session's sets, by its uuid
    val lastTime: Map<String, List<WorkoutSet>> = emptyMap(),
    // Each exercise's estimated 1RM in the user's unit, by its uuid; 0 without recent sets to estimate it from
    val oneRepMaxes: Map<String, Float> = emptyMap(),
    val editing: Boolean = false,
    val dialog: WorkoutLogDialog? = null,
    val exit: WorkoutLogExit? = null,
) {
    val elapsedSeconds: Long
        get() = ((now - startedAt) / 1000).coerceAtLeast(0)

    val unfinishedSets: Int
        get() = blocks.unfinishedSets()

    val mainKey: KeypadMainKey?
        get() {
            val focus = focus ?: return null
            val set = blocks.set(focus.ref) ?: return null
            return when {
                timerNext(focus.ref) && focus.field == ValueField.Time -> KeypadMainKey.StartTimer
                // A timed set not done yet goes on from its weight to its time, where the timer starts
                timerNext(focus.ref) && !set.done -> KeypadMainKey.Next
                set.mode == SetMode.Reps && focus.field != ValueField.Weight -> KeypadMainKey.Next
                set.done -> KeypadMainKey.Done
                else -> KeypadMainKey.CompleteSet
            }
        }

    // A done timed set can run its timer again
    private fun timerNext(ref: SetRef): Boolean {
        val set = blocks.set(ref) ?: return false
        return set.mode == SetMode.Time && !isTiming(ref) && ref !in timedSets
    }

    // What the closed keypad's main action completes (or times)
    val target: SetRef?
        get() = selected?.takeIf { blocks.set(it)?.done == false } ?: current

    val targetStartsTimer: Boolean
        get() = target?.let(::startsTimer) == true

    // A timed set's timer runs once; after that it's ticked like any set
    fun startsTimer(ref: SetRef): Boolean {
        val set = blocks.set(ref) ?: return false
        return set.mode == SetMode.Time && !set.done && !isTiming(ref) && ref !in timedSets
    }

    // Where the focused value came from, while it's still the suggestion. A 1RM goal sets only weights, and only
    // for exercises with a 1RM; everything else comes from last time.
    val suggestionNote: SuggestionNote?
        get() {
            val focus = focus ?: return null
            val set = blocks.set(focus.ref)?.takeIf { it.isSuggested(focus.field) } ?: return null
            val uuid = blocks.firstOrNull { it.id == focus.ref.blockId }?.exercises?.getOrNull(focus.ref.index)?.exercise?.uuid
            val fromGoal = focus.field == ValueField.Weight && suggestions.is1RMBased() && (oneRepMaxes[uuid] ?: 0f) > 0f
            return if (fromGoal) SuggestionNote.Goal(suggestions, set.weightKg ?: 0f) else SuggestionNote.LastTime
        }

    // Every exercise has the same rest, as the options change them all at once
    val restSeconds: Int
        get() = blocks.firstOrNull()?.restSeconds ?: 0

    fun isTiming(ref: SetRef): Boolean = (timer as? WorkoutTimer.SetTimer)?.ref == ref

    // Rounded up, so it shows 1 until it's done
    fun timerRemainingSeconds(): Int = timer?.let { ((it.endsAt - now).coerceAtLeast(0) + 999).toInt() / 1000 } ?: 0
}

/**
 * [totalSeconds] is how long it runs, with any time added or taken off.
 */
internal sealed interface WorkoutTimer {
    val endsAt: Long
    val totalSeconds: Int

    data class Rest(override val endsAt: Long, override val totalSeconds: Int) : WorkoutTimer

    data class SetTimer(val ref: SetRef, override val endsAt: Long, override val totalSeconds: Int) : WorkoutTimer
}

internal sealed interface SuggestionNote {
    data object LastTime : SuggestionNote

    data class Goal(val goal: MuscleGoal, val weightKg: Float) : SuggestionNote
}

// Reps, then the weight, whose key completes the set. A timed set's time starts its timer instead, then completes the
// set once the timer has run. A done set's values save as they're typed, so its key just moves on to the set to
// complete next, after running a timed set's timer again.
internal enum class KeypadMainKey {
    Next,
    CompleteSet,
    StartTimer,
    Done,
}

internal data class Deletion(val sets: DeletedSets, val current: SetRef?)

internal sealed interface WorkoutLogMessage {
    data class TooManySets(val max: Int) : WorkoutLogMessage
}

internal sealed interface WorkoutLogDialog {
    // Finish with sets not ticked
    data object Finish : WorkoutLogDialog

    // [untickedSaved]: the unticked sets that Finish would save as done
    data class Discard(val setsCompleted: Int, val untickedSaved: Int) : WorkoutLogDialog

    data class DeleteExercise(val blockId: Long) : WorkoutLogDialog

    data object RestTimer : WorkoutLogDialog
}

internal sealed interface WorkoutLogExit {
    data object Discarded : WorkoutLogExit

    data class Finished(val workout: ELWorkout) : WorkoutLogExit
}
