package com.everlog.ui.activities.workoutlog

import com.everlog.data.controllers.workoutsession.Completion
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
import com.everlog.data.model.set.ELSetType
import com.everlog.data.model.workout.ELWorkout
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.ui.mvvm.CommonViewModel
import com.everlog.utils.UnitUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Date

/**
 * The new workout screen (TAS-455): the ongoing workout as a table, typed into with a docked keypad. It does what
 * the old screen's PresenterWorkout does: the workout is saved as the ongoing one after every change, so it can be
 * resumed, and finishing saves it to Firestore and checks it off the plan.
 *
 * @param workout The workout as launched or resumed (see [load]), for everything but its exercises, which the state
 * holds
 * @param clock The current time, so tests can pin it
 * @param tickMillis How often the workout's time moves on; null stops it, for tests
 */
internal class WorkoutLogViewModel(
    dispatcher: CoroutineDispatcher,
    private val workout: ELWorkout,
    private val fromPlan: Boolean,
    private val store: WorkoutLogStore,
    private val analytics: WorkoutLogAnalytics,
    private val clock: () -> Long = System::currentTimeMillis,
    tickMillis: Long? = TickMillis,
) : CommonViewModel<WorkoutLogState>(dispatcher, initialState(workout, store, clock())) {

    private val ids = SessionIds(start = state.value.blocks.lastId())

    init {
        // So the workout can be resumed from the start, as the old screen does
        save()
        if (tickMillis != null) {
            launchCoroutine {
                while (isActive) {
                    delay(tickMillis)
                    setState { copy(now = clock()) }
                }
            }
        }
    }

    // A setting may have changed while the screen was away, e.g. the weight unit
    fun onResume() = setState { copy(unit = store.weightUnit(), now = clock()) }

    // The keypad's ± for a weight, in the user's unit
    fun weightIncrease(): Float = store.weightIncrease()

    // Values

    /**
     * @param value Reps, seconds, or a weight in the user's unit. Null clears it.
     */
    fun onSetValue(ref: SetRef, field: ValueField, value: Float?) = change {
        val stored = if (field == ValueField.Weight) value?.let { toKg(it, unit) } else value
        copy(blocks = blocks.setValue(ref, field, stored))
    }

    fun onSetMode(ref: SetRef, mode: SetMode) = change { copy(blocks = blocks.setMode(ref, mode)) }

    // Completing

    /**
     * A set's tick: completes it with the values it has, or undoes it.
     */
    fun onToggleSet(ref: SetRef) {
        val set = state.value.blocks.set(ref) ?: return
        if (set.done) {
            change { copy(blocks = blocks.uncompleteSet(ref), current = current ?: ref) }
            analytics.setEdited(setType(ref), set.mode == SetMode.Time)
        } else {
            onCompleteSet(ref)
        }
    }

    /**
     * Completes a set: the current one moves on to the next set not done. Rest comes with the timers (stage 3).
     */
    fun onCompleteSet(ref: SetRef) {
        val before = state.value
        val set = before.blocks.set(ref) ?: return
        if (set.done) return
        val completion: Completion = before.blocks.completeSet(ref, before.current, clock())
        change { copy(blocks = completion.blocks, current = completion.current) }
        analytics.setCompleted(setType(ref), set.mode == SetMode.Time)
        // A superset's round (or another set of several exercises) done with this set
        val round = completion.blocks.round(ref.blockId, ref.roundId)
        val exercises = round?.sets?.count { !it.removed } ?: 0
        if (round?.done == true && exercises > 1) {
            analytics.setGroupCompleted(setType(ref), exercises)
        }
    }

    // Adding

    fun onExercisesPicked(groups: List<ELExerciseGroup>) {
        if (groups.isEmpty()) return
        change {
            val blocks = blocks.addGroups(groups, ids)
            // Nothing was current (an empty workout, or every set done): the first new set is
            copy(blocks = blocks, current = current ?: blocks.nextOpenAfter(this.blocks.refs().lastOrNull()))
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

    private fun setType(ref: SetRef): ELSetType? =
        state.value.blocks.block(ref.blockId)?.type?.let { runCatching { ELSetType.valueOf(it) }.getOrNull() }

    companion object {
        private const val TickMillis = 1000L

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

        private fun initialState(workout: ELWorkout, store: WorkoutLogStore, now: Long): WorkoutLogState {
            val blocks = workout.toSessionBlocks(SessionIds())
            return WorkoutLogState(
                name = workout.getName().orEmpty(),
                startedAt = workout.createdDate,
                now = now,
                blocks = blocks,
                current = blocks.resumeAt(),
                unit = store.weightUnit(),
            )
        }

        // The next set not done after the one completed last, as the notification picks it
        private fun List<WorkoutBlock>.resumeAt(): SetRef? {
            val last = refs().filter { set(it)?.done == true }.maxByOrNull { set(it)?.completedDate ?: 0 }
            return nextOpenAfter(last)
        }

        private fun List<WorkoutBlock>.lastId(): Long = maxOfOrNull { block -> maxOf(block.id, block.rounds.maxOfOrNull { it.id } ?: 0) } ?: 0

        private fun toKg(value: Float, unit: WeightUnit): Float = if (unit == WeightUnit.POUND) UnitUtils.lbToKg(value) else value
    }
}

/**
 * A weight in kg, in the user's unit.
 */
internal fun displayWeight(kg: Float, unit: WeightUnit): Float = if (unit == WeightUnit.POUND) UnitUtils.kgToLb(kg) else kg
