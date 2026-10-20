package com.everlog.ui.activities.workoutlog

import com.everlog.data.controllers.workoutsession.SessionIds
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.addGroups
import com.everlog.data.controllers.workoutsession.block
import com.everlog.data.controllers.workoutsession.completeSet
import com.everlog.data.controllers.workoutsession.nextOpenAfter
import com.everlog.data.controllers.workoutsession.refs
import com.everlog.data.controllers.workoutsession.round
import com.everlog.data.controllers.workoutsession.set
import com.everlog.data.controllers.workoutsession.setMode
import com.everlog.data.controllers.workoutsession.setValue
import com.everlog.data.controllers.workoutsession.toSessionBlocks
import com.everlog.data.controllers.workoutsession.uncompleteSet
import com.everlog.data.controllers.workoutsession.withSessionBlocks
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.workout.ELWorkout
import com.everlog.ui.mvvm.CommonViewModel
import java.util.Date
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * The new workout screen (TAS-455): the ongoing workout as a table, typed into with a docked keypad. It does what
 * the old screen's PresenterWorkout does: the workout is saved as the ongoing one after every change, so it can be
 * resumed, and finishing saves it to Firestore and checks it off the plan.
 *
 * @param workout The workout as launched or resumed (see [load]), for everything but its exercises, which the state
 * holds
 * @param clock The current time, so tests can pin it
 * @param ids Ids for the blocks and rounds, the loaded ones and any added later
 * @param tickMillis How often the workout's time moves on while the screen shows; null stops it, for tests
 */
internal class WorkoutLogViewModel(
    dispatcher: CoroutineDispatcher,
    private val workout: ELWorkout,
    private val fromPlan: Boolean,
    private val store: WorkoutLogStore,
    private val settings: WorkoutLogSettings,
    private val analytics: WorkoutLogAnalytics,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ids: SessionIds = SessionIds(),
    private val tickMillis: Long? = TickMillis,
) : CommonViewModel<WorkoutLogState>(dispatcher, initialState(workout, ids, settings, clock())) {

    private var ticking: Job? = null

    init {
        // So the workout can be resumed from the start, as the old screen does
        save()
    }

    // A setting may have changed while the screen was away, e.g. the weight unit. The time only moves on while
    // the screen shows.
    fun onResume() {
        analytics.screenViewed()
        setState { withSettings(settings).copy(now = clock()) }
        val tick = tickMillis ?: return
        ticking?.cancel()
        ticking = launchCoroutine {
            while (isActive) {
                delay(tick)
                setState { copy(now = clock()) }
            }
        }
    }

    fun onPause() {
        ticking?.cancel()
        ticking = null
    }

    // The keypad

    fun onCellTapped(cell: CellFocus) = setState { focusOn(cell) }

    // Opens the keypad on the selected set's reps (or time)
    fun onShowKeypad() = setState { (selected ?: current)?.let { focusOn(countCell(it)) } ?: this }

    fun onHideKeypad() = setState { focusOn(null) }

    // Back closes the keypad first, then asks about leaving
    fun onBack() {
        if (state.value.focus != null) onHideKeypad() else onClose()
    }

    fun onDigit(digit: Char) = type(digit)

    fun onDecimal() = type('.')

    fun onBackspace() {
        val state = state.value
        val cell = state.focus ?: return
        val set = state.blocks.set(cell.ref) ?: return
        enter(cell, (state.typed ?: rawText(set, cell.field, state.unit)).dropLast(1))
    }

    // − or +: by a rep, 5 seconds or the weight step from Settings
    fun onStep(direction: Int) {
        val state = state.value
        val cell = state.focus ?: return
        val set = state.blocks.set(cell.ref) ?: return
        val (value, step) = when (cell.field) {
            ValueField.Weight -> (set.weightKg?.let { displayWeight(it, state.unit) } ?: 0f) to state.weightStep
            ValueField.Reps -> (set.reps ?: 0).toFloat() to RepsStep
            ValueField.Time -> (set.timeSeconds ?: 0).toFloat() to TimeStepSeconds
        }
        change { withValue(cell, (value + direction * step).coerceAtLeast(0f)).copy(typed = null, reveal = reveal + 1) }
    }

    // Reps or Time for the focused set. Typing reps or time carries on in the other.
    fun onModeSelected(mode: SetMode) {
        val cell = state.value.focus ?: return
        change {
            val switched = copy(blocks = blocks.setMode(cell.ref, mode))
            if (cell.field == ValueField.Weight) switched else switched.focusOn(CellFocus(cell.ref, if (mode == SetMode.Time) ValueField.Time else ValueField.Reps))
        }
    }

    fun onMainKey() {
        val cell = state.value.focus ?: return
        when (state.value.mainKey) {
            KeypadMainKey.Next -> setState { focusOn(CellFocus(cell.ref, ValueField.Weight)) }
            KeypadMainKey.Done -> setState { focusOn(null).copy(selected = current ?: selected) }
            // On to the next set's reps, or closed when there's none left
            KeypadMainKey.CompleteSet -> {
                complete(cell.ref)
                setState { focusOn(current?.let { countCell(it) }) }
            }
            null -> Unit
        }
    }

    // The closed keypad's Complete set
    fun onCompleteTarget() {
        state.value.target?.let(::complete)
    }

    private fun type(key: Char) {
        val state = state.value
        val cell = state.focus ?: return
        val base = state.typed.orEmpty()
        if (key == '.' && (cell.field != ValueField.Weight || base.contains('.'))) return
        enter(cell, (base + key).take(maxLength(cell.field)))
    }

    private fun enter(cell: CellFocus, text: String) =
        change { withValue(cell, parse(cell.field, text)).copy(typed = text, reveal = reveal + 1) }

    // Completing

    /**
     * A set's tick: selects it (the open keypad follows), then completes it with the values it has, or undoes it.
     */
    fun onTick(ref: SetRef) {
        setState { if (focus != null) focusOn(countCell(ref)) else copy(selected = ref) }
        val set = state.value.blocks.set(ref) ?: return
        if (set.done) {
            change { copy(blocks = blocks.uncompleteSet(ref), current = current ?: ref) }
            analytics.setEdited(state.value.blocks.block(ref.blockId)?.setType, set.mode == SetMode.Time)
        } else {
            complete(ref)
        }
    }

    /**
     * Completes a set: the current one moves on to the next set not done, and the selection follows it.
     */
    private fun complete(ref: SetRef) {
        val before = state.value
        val set = before.blocks.set(ref) ?: return
        if (set.done) return
        val completion = before.blocks.completeSet(ref, before.current, clock())
        change { copy(blocks = completion.blocks, current = completion.current).followCurrent() }
        val type = completion.blocks.block(ref.blockId)?.setType
        analytics.setCompleted(type, set.mode == SetMode.Time)
        // A superset's round (or another set of several exercises) done with this set
        val round = completion.blocks.round(ref.blockId, ref.roundId)
        val exercises = round?.sets?.count { !it.removed } ?: 0
        if (round?.done == true && exercises > 1) {
            analytics.setGroupCompleted(type, exercises)
        }
    }

    // Adding

    fun onExercisesPicked(groups: List<ELExerciseGroup>) {
        if (groups.isEmpty()) return
        change {
            val blocks = blocks.addGroups(groups, ids)
            // Nothing was current (an empty workout, or every set done): the first new set is
            copy(blocks = blocks, current = current ?: blocks.nextOpenAfter(this.blocks.refs().lastOrNull())).followCurrent()
        }
        analytics.exercisesAdded()
    }

    // Finishing

    // Asks first when sets aren't ticked: they're saved as they are, those with reps or a time as done
    fun onFinish() {
        if (state.value.unfinishedSets > 0) {
            setState { copy(dialog = WorkoutLogDialog.Finish) }
        } else {
            finish()
        }
    }

    fun onFinishConfirmed() = finish()

    private fun finish() {
        val finished = workout.withSessionBlocks(state.value.blocks, finished = true)
        finished.setCompletedDateAsDate(Date(clock()))
        store.clearOngoing()
        analytics.workoutCompleted()
        store.saveFinished(finished, fromPlan)
        setState { copy(dialog = null, exit = WorkoutLogExit.Finished(finished)) }
    }

    // Leaving

    // Close or Back: discard, or finish instead when sets have been done
    fun onClose() {
        val blocks = state.value.blocks
        val refs = blocks.refs()
        val completed = refs.count { blocks.set(it)?.done == true }
        val untickedSaved = refs.count { ref ->
            blocks.set(ref)?.let { !it.done && (if (it.mode == SetMode.Time) it.timeSeconds != null else it.reps != null) } == true
        }
        setState { copy(dialog = WorkoutLogDialog.Discard(completed, untickedSaved)) }
        analytics.discardPromptShown(completed)
    }

    fun onDiscardConfirmed() {
        store.clearOngoing()
        analytics.workoutStopped()
        setState { copy(dialog = null, exit = WorkoutLogExit.Discarded) }
    }

    // Choosing Finish over Discard already confirms it, so unlike Finish this doesn't ask about unticked sets
    fun onDiscardFinish() {
        analytics.discardPromptFinished()
        finish()
    }

    fun onDialogDismissed() {
        if (state.value.dialog is WorkoutLogDialog.Discard) analytics.discardPromptCancelled()
        setState { copy(dialog = null) }
    }

    // Saving

    // A change to the workout, saved as the ongoing one straight away
    private fun change(reducer: WorkoutLogState.() -> WorkoutLogState) {
        setState(reducer)
        save()
    }

    private fun save() {
        if (state.value.exit == null) {
            store.saveOngoing(workout.withSessionBlocks(state.value.blocks))
        }
    }

    companion object {
        private const val TickMillis = 1000L
        private const val RepsStep = 1f
        private const val TimeStepSeconds = 5f

        /**
         * The workout to show for the one the screen was opened with: its saved copy if it's the ongoing one, with
         * everything done since it started, otherwise the new workout with the routine's targets as its values.
         */
        fun load(launched: ELWorkout, store: WorkoutLogStore): ELWorkout {
            // The launch intent only has the workout as it was when it started. When Android rebuilds this screen
            // after the app was killed, or it's resumed, continue from the saved copy instead.
            val saved = store.savedCopyOf(launched)
            if (saved != null) {
                saved.clearRemainingTimes()
                return saved
            }
            // A new workout starts with the routine's targets as its values
            launched.prefillRequiredMetrics()
            return launched
        }

        private fun initialState(workout: ELWorkout, ids: SessionIds, settings: WorkoutLogSettings, now: Long): WorkoutLogState {
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
            )
        }

        private fun WorkoutLogState.withSettings(settings: WorkoutLogSettings) = copy(
            unit = settings.weightUnit(),
            weightStep = settings.weightIncrease(),
            keepScreenOn = settings.keepScreenOn(),
        )

        // The next set not done after the one completed last, as the notification picks it
        private fun List<WorkoutBlock>.resumeAt(): SetRef? {
            val last = refs().filter { set(it)?.done == true }.maxByOrNull { set(it)?.completedDate ?: 0 }
            return nextOpenAfter(last)
        }

    }
}

// Focuses a cell for the keypad, or closes it, starting afresh: the first key replaces the value. A focused set is
// the selected one.
private fun WorkoutLogState.focusOn(cell: CellFocus?) = copy(focus = cell, typed = null, selected = cell?.ref ?: selected)

// The set's reps or time, whichever it records
private fun WorkoutLogState.countCell(ref: SetRef): CellFocus? = blocks.set(ref)?.let { CellFocus(ref, countField(it)) }

// Sets one of the focused set's values. A weight is typed in the user's unit and stored in kg.
private fun WorkoutLogState.withValue(cell: CellFocus, value: Float?): WorkoutLogState {
    val stored = if (cell.field == ValueField.Weight) value?.let { storedWeight(it, unit) } else value
    return copy(blocks = blocks.setValue(cell.ref, cell.field, stored))
}

// Once the selected set is done, the selection (and the open keypad) moves on to the current set. Nothing selected
// yet, e.g. in an empty workout that just got its first exercise, selects the current set too.
private fun WorkoutLogState.followCurrent(): WorkoutLogState {
    val next = current ?: return this
    val selectedSet = selected?.let { blocks.set(it) }
    if (selectedSet != null && !selectedSet.done) return this
    return if (focus != null) focusOn(countCell(next)) else copy(selected = next)
}
