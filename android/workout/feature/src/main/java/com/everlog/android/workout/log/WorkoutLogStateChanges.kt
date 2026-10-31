package com.everlog.android.workout.log

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.session.SessionIds
import com.everlog.android.common.session.SetRef
import com.everlog.android.common.session.ValueField
import com.everlog.android.common.session.WorkoutBlock
import com.everlog.android.common.session.nextOpenAfter
import com.everlog.android.common.session.refs
import com.everlog.android.common.session.set
import com.everlog.android.common.session.setValue
import com.everlog.android.common.session.toSessionBlocks
import com.everlog.android.common.sessionui.exercisetable.CellFocus
import com.everlog.android.common.sessionui.exercisetable.countField
import com.everlog.android.common.kotlin.format.storedWeight

// How the workout screen's state changes, as plain functions the view-model builds its events from

internal fun initialWorkoutLogState(workout: ELWorkout, ids: SessionIds, settings: WorkoutLogSettings, now: Long): WorkoutLogState {
    val blocks = workout.toSessionBlocks(ids)
    return WorkoutLogState(
        name = workout.getName().orEmpty(),
        startedAt = workout.createdDate,
        now = now,
        blocks = blocks,
        current = blocks.resumeAt(),
        unit = settings.weightUnit(),
        weightStep = settings.weightIncrease(),
        keepScreenOn = settings.keepScreenOn(),
        suggestions = settings.muscleGoal(),
    )
}

internal fun WorkoutLogState.withSettings(settings: WorkoutLogSettings) = copy(
    unit = settings.weightUnit(),
    weightStep = settings.weightIncrease(),
    keepScreenOn = settings.keepScreenOn(),
    suggestions = settings.muscleGoal(),
)

// The notification picks the next set the same way
private fun List<WorkoutBlock>.resumeAt(): SetRef? {
    val last = refs().filter { set(it)?.done == true }.maxByOrNull { set(it)?.completedDate ?: 0 }
    return nextOpenAfter(last)
}

// The first key after focusing replaces the value
internal fun WorkoutLogState.focusOn(cell: CellFocus?) = copy(focus = cell, typed = null).select(cell?.ref ?: selected)

// A done set whose timer ran again offers it once more when it's next selected
internal fun WorkoutLogState.select(ref: SetRef?): WorkoutLogState {
    if (ref == selected) return this
    return copy(selected = ref, timedSets = timedSets.filterNot { blocks.set(it)?.done == true }.toSet())
}

internal fun WorkoutLogState.countCell(ref: SetRef): CellFocus? = blocks.set(ref)?.let { CellFocus(ref, countField(it)) }

// A weight is typed in the user's unit and stored in kg
internal fun WorkoutLogState.withValue(cell: CellFocus, value: Float?): WorkoutLogState {
    val stored = if (cell.field == ValueField.Weight) value?.let { storedWeight(it, unit) } else value
    return copy(blocks = blocks.setValue(cell.ref, cell.field, stored))
}

// Once the selected set is done, the selection (and the open keypad) moves on to the current set. So does an empty
// selection, e.g. in an empty workout that just got its first exercise.
internal fun WorkoutLogState.followCurrent(): WorkoutLogState {
    val next = current ?: return this
    val selectedSet = selected?.let { blocks.set(it) }
    if (selectedSet != null && !selectedSet.done) return this
    return if (focus != null) focusOn(countCell(next)) else select(next)
}

// A timed set whose timer has run is ticked next rather than timed again
internal fun WorkoutLogState.stopped(timer: WorkoutTimer) = copy(
    timer = null,
    timedSets = if (timer is WorkoutTimer.SetTimer) timedSets + timer.ref else timedSets,
)

// Anything on the sets that went goes too (the keypad, a set timer, the selection), and the current set moves on to
// the next one not done that's still there
internal fun WorkoutLogState.withSetsGone(remaining: List<WorkoutBlock>): WorkoutLogState {
    val kept = { it: SetRef? -> it != null && remaining.set(it) != null }
    val refs = blocks.refs()
    val from = refs.indexOf(current)
    val next = (refs.drop(from + 1) + refs.take(from + 1)).firstOrNull { remaining.set(it)?.done == false }
    return copy(
        blocks = remaining,
        current = current.takeIf(kept) ?: next,
        selected = selected.takeIf(kept),
        focus = focus?.takeIf { kept(it.ref) },
        typed = typed.takeIf { kept(focus?.ref) },
        timer = timer.takeUnless { it is WorkoutTimer.SetTimer && !kept(it.ref) },
    ).followCurrent()
}
