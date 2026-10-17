package com.everlog.ui.activities.workoutprototype

import com.everlog.ui.mvvm.CommonViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * The Spine and Dock workout screen's behaviour (the "Workout screen" page of the Everlog design),
 * on mock data held only here. Nothing is saved: finishing or leaving throws the workout away.
 *
 * Completing a set marks it done, moves on to the next set not done and starts the workout's one
 * rest. In a superset or circuit there's no rest within a round, and a round isn't left until all its
 * exercises are done: it goes back to the earliest one not done. A time set completes when
 * its timer ends (or Finish early), and rest then starts on its own. Tapping a set not done makes
 * it the current one; tapping a done one edits it in the dock until Save or Cancel.
 *
 * @param clock The current time, so tests can pin it
 */
internal class WorkoutPrototypeViewModel(
    dispatcher: CoroutineDispatcher,
    private val clock: () -> Long = System::currentTimeMillis,
) : CommonViewModel<WorkoutPrototypeState>(dispatcher, sampleState(clock(), IdGenerator())) {

    private val ids = IdGenerator(start = 10_000)

    init {
        launchCoroutine {
            while (isActive) {
                tick()
                delay(TickMillis)
            }
        }
    }

    // The timers: a set timer that ends completes its set, and a rest that ends goes back to the set
    private fun tick() {
        val now = clock()
        setState {
            var next = copy(now = now)
            val timer = next.setTimer
            if (timer != null && now >= timer.endsAt) {
                next = next.completeCurrent(now).copy(alerts = alerts + 1)
            }
            val rest = next.rest
            if (rest != null && now >= rest.endsAt) {
                next = next.copy(rest = null, alerts = next.alerts + 1)
            }
            next
        }
    }

    // Sets

    fun onSetTapped(ref: SetRef) = setState {
        val set = blocks.set(ref) ?: return@setState this
        when {
            // Already being edited: point at the dock, keeping the changes made so far
            editing?.ref == ref -> copy(dockNudges = dockNudges + 1)
            set.done -> copy(editing = EditingSet(ref, set.values))
            // Already current: point at the dock, or stop editing another set
            ref == current -> if (editing == null) copy(dockNudges = dockNudges + 1) else copy(editing = null)
            // Another set becomes current, so a running timer on the old one stops
            else -> copy(current = ref, editing = null, setTimer = null)
        }
    }

    fun onModeChanged(mode: SetMode) = updateDockValues { it.copy(mode = mode) }

    fun onRepsStep(delta: Int) = updateDockValues { it.copy(reps = (it.reps + delta).coerceAtLeast(0)) }

    fun onTimeStep(delta: Int) = updateDockValues { it.copy(timeSeconds = (it.timeSeconds + delta).coerceAtLeast(TimeStepSeconds)) }

    fun onWeightStep(delta: Float) = updateDockValues { it.copy(weightKg = (it.weightKg + delta).coerceAtLeast(0f)) }

    fun onEnterValue(field: ValueField) = setState { copy(dialog = WorkoutDialog.EnterValue(field)) }

    fun onValueEntered(field: ValueField, value: Float) {
        setState { copy(dialog = null) }
        updateDockValues {
            when (field) {
                ValueField.Reps -> it.copy(reps = value.toInt().coerceAtLeast(0))
                ValueField.Time -> it.copy(timeSeconds = value.toInt().coerceAtLeast(1))
                ValueField.Weight -> it.copy(weightKg = value.coerceAtLeast(0f))
            }
        }
    }

    // The dock's steppers change the set being edited, or else the current set
    private fun updateDockValues(transform: (SetValues) -> SetValues) = setState {
        val editing = editing
        when {
            editing != null -> copy(editing = editing.copy(values = transform(editing.values)))
            current != null && setTimer == null -> copy(blocks = blocks.updateSet(current) { it.copy(values = transform(it.values)) })
            else -> this
        }
    }

    fun onCompleteSet() = setState { completeCurrent(clock()) }

    // A set's tick. Not done: completes it with the values shown, every exercise of a superset's round
    // at once, and rest starts. The current set moves on only if it was one of them. Done: undoes it.
    // The tick of the set being edited saves it, like Done.
    fun onToggleRound(blockId: Long, roundId: Long) = setState {
        val editing = editing
        if (editing != null && editing.ref.roundId == roundId) {
            return@setState copy(blocks = blocks.updateSet(editing.ref) { it.copy(values = editing.values) }, editing = null)
        }
        val round = block(blockId)?.rounds?.firstOrNull { it.id == roundId } ?: return@setState this
        val inRound = { ref: SetRef? -> ref?.roundId == roundId }
        val done = round.sets.all { it.done }
        val blocks = blocks.map { block ->
            if (block.id != blockId) return@map block
            block.copy(rounds = block.rounds.map { if (it.id == roundId) it.copy(sets = it.sets.map { set -> set.copy(done = !done) }) else it })
        }
        if (done) {
            return@setState copy(blocks = blocks, current = current ?: SetRef(blockId, roundId, 0))
        }
        val now = clock()
        val current = if (current == null || inRound(current)) blocks.nextOpenAfter(SetRef(blockId, roundId, round.sets.lastIndex)) else current
        copy(
            blocks = blocks,
            current = current,
            setTimer = setTimer?.takeUnless { inRound(it.ref) },
            editing = editing?.takeUnless { inRound(it.ref) },
            rest = if (current != null) RestTimer(endsAt = now + restSeconds * 1000L, totalSeconds = restSeconds) else rest,
            now = now,
        )
    }

    fun onSaveEdit() = setState {
        val editing = editing ?: return@setState this
        copy(blocks = blocks.updateSet(editing.ref) { it.copy(values = editing.values) }, editing = null)
    }

    fun onCancelEdit() = setState { copy(editing = null) }

    // Runs a done timed set again: with the edited values, not done, current, its timer counting down
    fun onRestartTimer() = setState {
        val editing = editing ?: return@setState this
        val now = clock()
        copy(
            blocks = blocks.updateSet(editing.ref) { it.copy(values = editing.values, done = false) },
            current = editing.ref,
            editing = null,
            rest = null,
            setTimer = SetTimer(editing.ref, endsAt = now + editing.values.timeSeconds * 1000L, totalSeconds = editing.values.timeSeconds),
            now = now,
        )
    }

    // Time sets

    fun onStartTimer() = setState {
        val ref = current ?: return@setState this
        val set = blocks.set(ref) ?: return@setState this
        val now = clock()
        copy(setTimer = SetTimer(ref, endsAt = now + set.values.timeSeconds * 1000L, totalSeconds = set.values.timeSeconds), rest = null, now = now)
    }

    // ±10s changes this countdown and the set's time with it, so running it again uses the new time.
    // The time doesn't go under the time stepper's step; a countdown that runs out completes the set.
    fun onTimerStep(deltaSeconds: Int) = setState {
        val timer = setTimer ?: return@setState this
        val time = blocks.set(timer.ref)?.values?.timeSeconds ?: return@setState this
        val newTime = (time + deltaSeconds).coerceAtLeast(TimeStepSeconds)
        val change = newTime - time
        copy(
            setTimer = timer.copy(endsAt = timer.endsAt + change * 1000L, totalSeconds = timer.totalSeconds + change),
            blocks = blocks.updateSet(timer.ref) { it.copy(values = it.values.copy(timeSeconds = newTime)) },
        )
    }

    // Completes the set before its timer ends, keeping its time, so running it again starts from
    // the full time
    fun onFinishTimerEarly() = setState { completeCurrent(clock()) }

    // Any set, for the table prototype's cells and keypad

    fun onSetValue(ref: SetRef, field: ValueField, value: Float) = setState {
        copy(blocks = blocks.updateSet(ref) { set ->
            set.copy(values = when (field) {
                ValueField.Reps -> set.values.copy(reps = value.toInt().coerceAtLeast(0))
                ValueField.Time -> set.values.copy(timeSeconds = value.toInt().coerceAtLeast(0))
                ValueField.Weight -> set.values.copy(weightKg = value.coerceAtLeast(0f))
            })
        })
    }

    fun onSetMode(ref: SetRef, mode: SetMode) = setState {
        copy(blocks = blocks.updateSet(ref) { it.copy(values = it.values.copy(mode = mode)) })
    }

    // Completes one exercise's set. The current one moves on as Complete set does; another is just
    // marked done, and rest starts once its round is done.
    fun onCompleteSetAt(ref: SetRef) = setState {
        val set = blocks.set(ref) ?: return@setState this
        if (set.done) return@setState this
        val now = clock()
        if (ref == current) return@setState completeCurrent(now)
        val blocks = blocks.updateSet(ref) { it.copy(done = true) }
        val roundDone = blocks.firstOrNull { it.id == ref.blockId }?.rounds?.firstOrNull { it.id == ref.roundId }?.sets?.all { it.done } == true
        val current = current ?: blocks.nextOpenAfter(ref)
        copy(
            blocks = blocks,
            current = current,
            setTimer = setTimer?.takeUnless { it.ref == ref },
            rest = if (roundDone && current != null) RestTimer(endsAt = now + restSeconds * 1000L, totalSeconds = restSeconds) else rest,
            now = now,
        )
    }

    // A row's tick: completes it, or undoes it
    fun onToggleSetAt(ref: SetRef) {
        val set = state.value.blocks.set(ref) ?: return
        if (set.done) {
            setState { copy(blocks = blocks.updateSet(ref) { it.copy(done = false) }, current = current ?: ref) }
        } else {
            onCompleteSetAt(ref)
        }
    }

    // Runs a timed set's timer, making it the current set
    fun onStartTimerAt(ref: SetRef) = setState {
        val set = blocks.set(ref) ?: return@setState this
        val now = clock()
        copy(
            current = ref,
            editing = null,
            rest = null,
            setTimer = SetTimer(ref, endsAt = now + set.values.timeSeconds * 1000L, totalSeconds = set.values.timeSeconds),
            now = now,
        )
    }

    // Rest

    // Changes this rest, and the workout's rest for every one after it
    fun onRestStep(deltaSeconds: Int) = setState {
        val rest = rest ?: return@setState this
        val now = clock()
        val endsAt = rest.endsAt + deltaSeconds * 1000L
        copy(
            rest = if (endsAt <= now) null else rest.copy(endsAt = endsAt, totalSeconds = (rest.totalSeconds + deltaSeconds).coerceAtLeast(1)),
            restSeconds = (restSeconds + deltaSeconds).coerceAtLeast(RestStepSeconds),
            now = now,
        )
    }

    fun onSkipRest() = setState { copy(rest = null) }

    fun onRestTimerSettings() = setState { copy(dialog = WorkoutDialog.RestTimer) }

    fun onRestSecondsStep(deltaSeconds: Int) = setState { copy(restSeconds = (restSeconds + deltaSeconds).coerceAtLeast(RestStepSeconds)) }

    // Adding and deleting

    fun onAddExercise() = setState {
        val block = WorkoutPrototypeSamples.randomBlock(ids::next, exclude = blocks.flatMap { it.exercises }.map { it.name }.toSet())
        val blocks = blocks + block
        copy(
            blocks = blocks,
            startedAt = startedAt ?: clock(),
            current = current ?: blocks.nextOpenAfter(current),
        )
    }

    // A new set after the last, with the last set's values
    fun onAddSet(blockId: Long) = setState {
        val blocks = blocks.map { block ->
            if (block.id != blockId) return@map block
            val last = block.rounds.lastOrNull() ?: return@map block
            block.copy(rounds = block.rounds + PrototypeRound(ids.next(), last.sets.map { PrototypeSet(it.values) }))
        }
        copy(blocks = blocks, current = current ?: blocks.nextOpenAfter(null))
    }

    // Swiping a set away. Deleting the current set moves current on to the next one.
    fun onDeleteRound(blockId: Long, roundId: Long) = setState {
        val block = block(blockId) ?: return@setState this
        val position = block.rounds.indexOfFirst { it.id == roundId }
        if (position < 0) return@setState this
        val deleted = DeletedRound(blockId, position, block.rounds[position], current)
        val blocks = blocks.map { if (it.id == blockId) it.copy(rounds = it.rounds.filterNot { round -> round.id == roundId }) else it }
        val inDeleted = { ref: SetRef? -> ref?.roundId == roundId }
        copy(
            blocks = blocks,
            current = if (inDeleted(current)) nextOpenAfterDeleted(this.blocks, current, roundId) else current,
            setTimer = setTimer?.takeUnless { inDeleted(it.ref) },
            editing = editing?.takeUnless { inDeleted(it.ref) },
            deletedRound = deleted,
        )
    }

    fun onUndoDelete() = setState {
        val deleted = deletedRound ?: return@setState this
        val blocks = blocks.map { block ->
            if (block.id != deleted.blockId) return@map block
            val rounds = block.rounds.toMutableList().apply { add(deleted.position.coerceAtMost(size), deleted.round) }
            block.copy(rounds = rounds)
        }
        copy(blocks = blocks, current = if (deleted.current?.roundId == deleted.round.id) deleted.current else current, deletedRound = null)
    }

    fun onUndoGone() = setState { copy(deletedRound = null) }

    fun onDeleteBlock(blockId: Long) = setState { copy(dialog = WorkoutDialog.DeleteBlock(blockId)) }

    fun onDeleteBlockConfirmed(blockId: Long) = setState {
        val blocks = blocks.filterNot { it.id == blockId }
        val inDeleted = { ref: SetRef? -> ref?.blockId == blockId }
        copy(
            blocks = blocks,
            current = if (inDeleted(current)) nextOpenAfterDeleted(this.blocks, current, roundId = null, blockId = blockId) else current,
            setTimer = setTimer?.takeUnless { inDeleted(it.ref) },
            editing = editing?.takeUnless { inDeleted(it.ref) },
            rest = rest.takeIf { blocks.isNotEmpty() },
            dialog = null,
        )
    }

    fun onExerciseDetails(exercise: PrototypeExercise) = setState { copy(message = "${exercise.name}'s details and history aren't in the prototype yet") }

    fun onHistory() = setState { copy(message = "History isn't in the prototype yet") }

    // Edit mode

    fun onEditWorkout() = setState { copy(editMode = true, editing = null) }

    fun onEditDone() = setState { copy(editMode = false) }

    fun onMoveBlock(from: Int, to: Int) = setState {
        if (from !in blocks.indices || to !in blocks.indices) return@setState this
        copy(blocks = blocks.toMutableList().apply { add(to, removeAt(from)) })
    }

    // Finishing and leaving

    // Asks once when sets aren't done: those are dropped and the rest would be saved
    fun onFinish() = setState {
        val unfinished = unfinishedSets()
        if (unfinished > 0) copy(dialog = WorkoutDialog.Finish(unfinished)) else finish()
    }

    fun onFinishConfirmed() = setState { finish() }

    private fun WorkoutPrototypeState.finish() = copy(dialog = null, message = "Workout finished. The prototype doesn't save it.", finished = true)

    fun onDiscard() = setState { copy(dialog = WorkoutDialog.Discard) }

    fun onDiscardConfirmed() = setState { copy(dialog = null, finished = true) }

    // Debug only: back to the sample, or an empty workout
    fun onResetToSample() = setState { sampleState(clock(), ids) }

    fun onStartEmpty() = setState { emptyState(clock()) }

    fun onDialogDismissed() = setState { copy(dialog = null) }

    fun onMessageShown() = setState { copy(message = null) }

    private fun nextOpenAfterDeleted(before: List<PrototypeBlock>, current: SetRef?, roundId: Long?, blockId: Long? = null): SetRef? {
        val refs = before.refs()
        val start = refs.indexOf(current)
        val ordered = refs.drop(start + 1) + refs.take(start + 1)
        return ordered.firstOrNull { it.roundId != roundId && it.blockId != blockId && before.set(it)?.done == false }
    }

    private class IdGenerator(start: Long = 0) {
        private var last = start

        fun next(): Long = ++last
    }

    companion object {
        const val RepsStep = 1
        const val WeightStepKg = 1f
        const val TimeStepSeconds = 5
        const val TimerStepSeconds = 10
        const val RestStepSeconds = 10
        private const val TickMillis = 200L

        private fun sampleState(now: Long, ids: IdGenerator): WorkoutPrototypeState {
            val blocks = WorkoutPrototypeSamples.pushDay(ids::next)
            return WorkoutPrototypeState(
                name = "Push day",
                startedAt = now,
                now = now,
                blocks = blocks,
                current = blocks.nextOpenAfter(null),
            )
        }

        private fun emptyState(now: Long) = WorkoutPrototypeState(name = "Push day", startedAt = null, now = now, blocks = emptyList(), current = null)
    }
}

// Marks the current set done and moves on. In a superset or circuit, a round with exercises still
// to do goes back to the earliest of them, without rest. Once the round is done it moves on to the
// next set not done, and rest starts, unless that was the last set of the workout.
internal fun WorkoutPrototypeState.completeCurrent(now: Long): WorkoutPrototypeState {
    val ref = current ?: return this
    val blocks = blocks.updateSet(ref) { it.copy(done = true) }
    val round = blocks.firstOrNull { it.id == ref.blockId }?.rounds?.firstOrNull { it.id == ref.roundId }
    val leftInRound = round?.sets?.indexOfFirst { !it.done }?.takeIf { it >= 0 }
    if (leftInRound != null) {
        return copy(blocks = blocks, current = SetRef(ref.blockId, ref.roundId, leftInRound), setTimer = null, editing = null, now = now)
    }
    val next = blocks.nextOpenAfter(ref)
    return copy(
        blocks = blocks,
        current = next,
        setTimer = null,
        editing = null,
        rest = if (next != null) RestTimer(endsAt = now + restSeconds * 1000L, totalSeconds = restSeconds) else rest,
        now = now,
    )
}
