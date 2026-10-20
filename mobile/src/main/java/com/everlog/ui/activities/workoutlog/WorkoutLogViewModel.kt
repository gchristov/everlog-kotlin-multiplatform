package com.everlog.ui.activities.workoutlog

import com.everlog.config.AppConfig
import com.everlog.data.controllers.workoutprefill.BaseWorkoutPrefillController.PrefillSource
import com.everlog.data.controllers.workoutsession.SessionIds
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.controllers.workoutsession.addGroups
import com.everlog.data.controllers.workoutsession.addRound
import com.everlog.data.controllers.workoutsession.block
import com.everlog.data.controllers.workoutsession.completeSet
import com.everlog.data.controllers.workoutsession.deleteBlock
import com.everlog.data.controllers.workoutsession.deleteSet
import com.everlog.data.controllers.workoutsession.moveBlock
import com.everlog.data.controllers.workoutsession.nextOpenAfter
import com.everlog.data.controllers.workoutsession.refs
import com.everlog.data.controllers.workoutsession.restore
import com.everlog.data.controllers.workoutsession.round
import com.everlog.data.controllers.workoutsession.set
import com.everlog.data.controllers.workoutsession.setMode
import com.everlog.data.controllers.workoutsession.setValue
import com.everlog.data.controllers.workoutsession.toELSet
import com.everlog.data.controllers.workoutsession.toSessionBlocks
import com.everlog.data.controllers.workoutsession.toSessionSet
import com.everlog.data.controllers.workoutsession.uncompleteSet
import com.everlog.data.controllers.workoutsession.withRestSeconds
import com.everlog.data.controllers.workoutsession.withSessionBlocks
import com.everlog.data.model.exercise.ELExercise
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import com.everlog.data.model.workout.ELWorkoutState
import com.everlog.ui.mvvm.CommonViewModel
import java.util.Date
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import timber.log.Timber

/**
 * The new workout screen (TAS-455): the ongoing workout as a table, typed into with a docked keypad. It does what
 * the old screen's PresenterWorkout does: the workout is saved as the ongoing one after every change, so it can be
 * resumed, and finishing saves it to Firestore and checks it off the plan.
 *
 * @param workout The workout as launched or resumed (see [load]), for everything but its exercises, which the state
 * holds
 * @param resumed Whether [workout] is a saved copy being resumed, whose sets were filled in from history already
 * @param clock The current time, so tests can pin it
 * @param ids Ids for the blocks and rounds, the loaded ones and any added later
 * @param tickMillis How often the workout's time moves on while the screen shows; null stops it, for tests
 */
internal class WorkoutLogViewModel(
    dispatcher: CoroutineDispatcher,
    private val workout: ELWorkout,
    private val resumed: Boolean,
    private val fromPlan: Boolean,
    private val store: WorkoutLogStore,
    private val settings: WorkoutLogSettings,
    private val analytics: WorkoutLogAnalytics,
    private val alerts: WorkoutLogAlerts,
    private val notification: WorkoutLogNotification,
    private val prefill: WorkoutLogPrefill,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ids: SessionIds = SessionIds(),
    private val tickMillis: Long? = TickMillis,
) : CommonViewModel<WorkoutLogState>(dispatcher, initialState(workout, ids, settings, clock())) {

    private var ticking: Job? = null
    // Whether the screen is showing, which keeps the time ticking
    private var showing = false

    // What each exercise is filled in from, by its uuid, once the user's history has loaded
    private var sources: Map<String, PrefillSource> = emptyMap()

    init {
        // So the workout can be resumed from the start, as the old screen does
        save()
        // A resumed workout was filled in when it started, and the user may have cleared values since
        loadHistory(state.value.blocks, fill = !resumed)
    }

    // A setting may have changed while the screen was away, e.g. the weight unit, which the notification shows too
    fun onResume() {
        showing = true
        analytics.screenViewed()
        setState { withSettings(settings).copy(now = clock()) }
        notifyWorkout()
        keepTicking()
    }

    // The time stops while the screen's away, unless a countdown is running: a rest still sounds with the phone
    // locked, as on the old screen
    fun onPause() {
        showing = false
    }

    /**
     * Moves the time on, and ends a countdown that has run out, with a sound. A timed set is left to be ticked, as on
     * the old screen.
     */
    fun tick() {
        setState { copy(now = clock()) }
        val timer = state.value.timer ?: return
        if (clock() < timer.endsAt) {
            // The notification counts down with the screen
            when (timer) {
                is WorkoutTimer.Rest -> notification.showRest(remainingPercent(timer), state.value.timerRemainingSeconds())
                is WorkoutTimer.SetTimer -> notifyWorkout()
            }
            return
        }
        setState { stopped(timer) }
        alerts.timerRanOut()
        notifyWorkout()
    }

    private fun remainingPercent(timer: WorkoutTimer): Int =
        if (timer.totalSeconds <= 0) 0 else (state.value.timerRemainingSeconds() * 100 / timer.totalSeconds).coerceIn(0, 100)

    private fun keepTicking() {
        val tick = tickMillis ?: return
        if (ticking?.isActive == true) return
        ticking = launchCoroutine {
            while (isActive && (showing || state.value.timer != null)) {
                delay(tick)
                tick()
            }
        }
    }

    // The keypad

    fun onCellTapped(cell: CellFocus) = setState { focusOn(cell) }

    // Opens the keypad on the selected set's reps (or time)
    fun onShowKeypad() = setState { (selected ?: current)?.let { focusOn(countCell(it)) } ?: this }

    fun onHideKeypad() = setState { focusOn(null) }

    // Back closes the keypad or Edit workout first, then asks about leaving
    fun onBack() {
        when {
            state.value.editing -> onEditDone()
            state.value.focus != null -> onHideKeypad()
            else -> onClose()
        }
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
            KeypadMainKey.StartTimer -> onStartSetTimer(cell.ref)
            null -> Unit
        }
    }

    // The closed keypad's main action: a timed set's timer starts first, then Complete set completes it
    fun onCompleteTarget() {
        val state = state.value
        val target = state.target ?: return
        if (state.targetStartsTimer) onStartSetTimer(target) else complete(target)
    }

    // Timers

    /**
     * Runs a timed set's timer for its time, making it the current set. Without a time there's nothing to count down,
     * so the keypad opens on it instead.
     */
    fun onStartSetTimer(ref: SetRef) = startSetTimer(ref, fromNotification = false)

    private fun startSetTimer(ref: SetRef, fromNotification: Boolean) {
        val set = state.value.blocks.set(ref) ?: return
        val seconds = set.timeSeconds ?: return setState { focusOn(CellFocus(ref, ValueField.Time)) }
        change {
            copy(
                current = ref,
                selected = ref,
                timer = WorkoutTimer.SetTimer(ref, endsAt = clock() + seconds * 1000L, totalSeconds = seconds),
                now = clock(),
            )
        }
        // WorkoutService logs the notification's buttons itself
        if (!fromNotification) analytics.setTimerStarted()
        keepTicking()
    }

    /**
     * −10s or +10s on the running countdown. A rest's change carries on to every rest after it, a set timer's to
     * the set's time, as on the old screen.
     */
    fun onTimerStep(direction: Int) {
        val timer = state.value.timer ?: return
        val offset = direction * TimerStepSeconds
        val total = (timer.totalSeconds + offset).coerceAtLeast(0)
        when (timer) {
            // Taken down to nothing, it only ends this rest: the ones after keep theirs, as on the old screen
            is WorkoutTimer.Rest -> change {
                copy(
                    timer = timer.copy(endsAt = timer.endsAt + offset * 1000L, totalSeconds = total),
                    blocks = if (total > 0) blocks.withRestSeconds(total) else blocks,
                )
            }
            is WorkoutTimer.SetTimer -> change {
                copy(
                    timer = timer.copy(endsAt = timer.endsAt + offset * 1000L, totalSeconds = total),
                    blocks = blocks.setValue(timer.ref, ValueField.Time, total.toFloat()),
                )
            }
        }
        // Taking the last seconds off ends it
        tick()
    }

    /**
     * Skip on a rest, Stop on a set timer: stops it early, without a sound. The set is left to be ticked.
     */
    fun onTimerEnd() = endTimer(fromNotification = false)

    private fun endTimer(fromNotification: Boolean) {
        val timer = state.value.timer ?: return
        setState { stopped(timer) }
        notifyWorkout()
        if (fromNotification) return
        when (timer) {
            is WorkoutTimer.Rest -> analytics.restStopped()
            is WorkoutTimer.SetTimer -> analytics.setTimerStopped()
        }
    }

    // The notification, shown while the screen is open (it stops when the screen closes for good)

    fun onScreenStarted() = notification.start(notificationWorkout())

    fun onScreenClosed() = notification.stop()

    // Its buttons, each for the set it shows: [position] is that set in the workout it was last sent

    fun onNotificationCompleteSet(position: ELWorkoutState) {
        setAt(position)?.let { complete(it, fromNotification = true) }
    }

    fun onNotificationWeight(position: ELWorkoutState, direction: Int) {
        val ref = setAt(position) ?: return
        val set = state.value.blocks.set(ref) ?: return
        val weight = set.weightKg?.let { displayWeight(it, state.value.unit) } ?: 0f
        change { withValue(CellFocus(ref, ValueField.Weight), (weight + direction * weightStep).coerceAtLeast(0f)) }
    }

    fun onNotificationReps(position: ELWorkoutState, direction: Int) {
        val ref = setAt(position) ?: return
        val reps = state.value.blocks.set(ref)?.reps ?: 0
        change { withValue(CellFocus(ref, ValueField.Reps), (reps + direction * RepsStep).coerceAtLeast(0f)) }
    }

    fun onNotificationSetTimer(position: ELWorkoutState, start: Boolean) {
        val ref = setAt(position) ?: return
        if (start) startSetTimer(ref, fromNotification = true) else if (state.value.isTiming(ref)) endTimer(fromNotification = true)
    }

    fun onNotificationRestStop() {
        if (state.value.timer is WorkoutTimer.Rest) endTimer(fromNotification = true)
    }

    // The set at a position in the workout as the notification has it: its group, its round and its place among the
    // exercises that have a set in that round
    private fun setAt(position: ELWorkoutState): SetRef? {
        val blocks = state.value.blocks
        val block = blocks.getOrNull(position.groupIndex) ?: return null
        val round = block.rounds.getOrNull(position.setIndex) ?: return null
        val group = workout.withSessionBlocks(blocks).getExerciseGroups().getOrNull(position.groupIndex) ?: return null
        val exercise = group.getExercisesForSetIndex(position.setIndex).getOrNull(position.exerciseIndex) ?: return null
        val index = group.exercises.indexOfFirst { it === exercise }
        return SetRef(block.id, round.id, index).takeIf { index >= 0 }
    }

    private val weightStep: Float
        get() = state.value.weightStep

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
    private fun complete(ref: SetRef, fromNotification: Boolean = false) {
        val before = state.value
        val set = before.blocks.set(ref) ?: return
        if (set.done) return
        val completion = before.blocks.completeSet(ref, before.current, clock())
        // The workout's rest starts once a set (or a superset's round) is done, replacing any countdown running.
        // Completing another set ends a running set timer.
        val restSeconds = completion.blocks.block(ref.blockId)?.restSeconds ?: 0
        val rest = if (completion.startsRest && restSeconds > 0) {
            WorkoutTimer.Rest(endsAt = clock() + restSeconds * 1000L, totalSeconds = restSeconds)
        } else {
            null
        }
        change {
            copy(
                blocks = completion.blocks,
                current = completion.current,
                timer = rest ?: timer.takeUnless { it is WorkoutTimer.SetTimer },
                now = clock(),
            ).followCurrent()
        }
        val type = completion.blocks.block(ref.blockId)?.setType
        analytics.setCompleted(type, set.mode == SetMode.Time, fromNotification)
        // A superset's round (or another set of several exercises) done with this set
        val round = completion.blocks.round(ref.blockId, ref.roundId)
        val exercises = round?.sets?.count { !it.removed } ?: 0
        if (round?.done == true && exercises > 1) {
            analytics.setGroupCompleted(type, exercises, fromNotification)
        }
        if (rest != null) {
            analytics.restStarted()
            keepTicking()
        }
    }

    // Adding and deleting sets

    /**
     * A new set at the end of an exercise (or a new round of a superset), with the last set's values, up to the old
     * screen's limit.
     */
    fun onAddSet(blockId: Long) {
        val block = state.value.blocks.block(blockId) ?: return
        if (block.rounds.size >= MaxSets) {
            setState { copy(message = WorkoutLogMessage.TooManySets(MaxSets)) }
            return
        }
        val blocks = state.value.blocks.addRound(blockId, ids).fillAdded(blockId)
        // Every set was done: the new one is current
        change { copy(blocks = blocks, current = current ?: blocks.nextOpenAfter(null)).followCurrent() }
        analytics.setAdded()
    }

    /**
     * Deletes one exercise's set, the whole exercise with its last set, until Undo. A countdown or the keypad on it
     * goes, and the current set moves on.
     */
    fun onDeleteSet(ref: SetRef) {
        val before = state.value
        val (blocks, deleted) = before.blocks.deleteSet(ref) ?: return
        change(keepUndo = true) { withSetsGone(blocks).copy(deletion = Deletion(deleted, before.current)) }
        analytics.setDeleted()
    }

    fun onUndoDelete() {
        val deletion = state.value.deletion ?: return
        change { copy(blocks = blocks.restore(deletion.sets), current = deletion.current ?: current) }
    }

    // The undo's message went without Undo
    fun onUndoGone() = setState { copy(deletion = null) }

    fun onMessageShown() = setState { copy(message = null) }

    // Exercises

    // Its menu's Delete, which asks first: its sets go with it
    fun onDeleteExercise(blockId: Long) = setState { copy(dialog = WorkoutLogDialog.DeleteExercise(blockId)) }

    fun onDeleteExerciseConfirmed(blockId: Long) {
        change {
            val remaining = withSetsGone(blocks.deleteBlock(blockId))
            // No rest without exercises to rest between, nor anything left to edit
            val any = remaining.blocks.isNotEmpty()
            remaining.copy(timer = remaining.timer.takeIf { any }, editing = editing && any, dialog = null)
        }
    }

    /**
     * An exercise changed in the library or by the user, e.g. renamed, shown as it is now, as the old screen does.
     */
    fun onExerciseChanged(exercise: ELExercise) {
        val uuid = exercise.uuid ?: return
        if (state.value.blocks.none { block -> block.exercises.any { it.exercise?.uuid == uuid } }) return
        change {
            copy(blocks = blocks.map { block ->
                block.copy(exercises = block.exercises.map { if (it.exercise?.uuid == uuid) it.copy(exercise = exercise) else it })
            })
        }
    }

    // Edit workout: reordering and deleting exercises

    fun onEditWorkout() = setState { focusOn(null).copy(editing = true) }

    fun onEditDone() = setState { copy(editing = false) }

    // Drags an exercise (a superset moves as one) from one place to another
    fun onMoveExercise(from: Int, to: Int) = change { copy(blocks = blocks.moveBlock(from, to)) }

    // The rest after each set

    fun onRestTimerSettings() = setState { copy(dialog = WorkoutLogDialog.RestTimer) }

    // −10s or +10s on every exercise's rest, not below 10s. A rest already running keeps its time.
    fun onRestStep(direction: Int) {
        val seconds = (state.value.restSeconds + direction * TimerStepSeconds).coerceAtLeast(TimerStepSeconds)
        change { copy(blocks = blocks.withRestSeconds(seconds)) }
        analytics.restChanged(seconds)
    }

    // Adding exercises

    fun onExercisesPicked(groups: List<ELExerciseGroup>) {
        if (groups.isEmpty()) return
        val before = state.value.blocks
        change {
            val blocks = blocks.addGroups(groups, ids)
            // Nothing was current (an empty workout, or every set done): the first new set is
            copy(blocks = blocks, current = current ?: blocks.nextOpenAfter(this.blocks.refs().lastOrNull())).followCurrent()
        }
        analytics.exercisesAdded()
        // Only the new exercises, so values cleared in the others aren't filled in again
        loadHistory(state.value.blocks.filter { block -> before.none { it.id == block.id } }, fill = true)
    }

    // Filling in from history

    /**
     * Loads what the exercises in [blocks] are filled in from, for their last time and for sets added later. With
     * [fill], and when the muscle goal fills sets in, it fills their empty values too, as the old screen does. A
     * weight filled in shows as a suggestion until it's changed.
     */
    private fun loadHistory(blocks: List<WorkoutBlock>, fill: Boolean) {
        val exercises = blocks.flatMap { block -> block.exercises.mapNotNull { it.exercise } }.distinctBy { it.uuid }
        if (exercises.isEmpty()) return
        launchCoroutine {
            val found = try {
                prefill.sources(exercises)
            } catch (e: Exception) {
                // Without history the sets keep the values they have, as on the old screen
                Timber.tag(TAG).e(e, "Couldn't load history to fill in from")
                return@launchCoroutine
            }
            sources = sources + found
            setState { copy(lastTime = lastTime + found.mapValues { (_, source) -> source.lastSets() }) }
            if (fill && prefill.fillsSets()) {
                val ids = blocks.map { it.id }.toSet()
                change { copy(blocks = this.blocks.map { if (it.id in ids) it.filled() else it }) }
            }
        }
    }

    // Every set's empty values filled in from its exercise's source
    private fun WorkoutBlock.filled(): WorkoutBlock = copy(rounds = rounds.mapIndexed { index, round ->
        round.copy(sets = round.sets.mapIndexed { i, set ->
            val source = sources[exercises[i].exercise?.uuid]
            if (set.removed || set.done || source == null) set else set.filledFrom { prefill.fill(source, it, index) }
        })
    })

    // A block's set just added filled in from the last session's set at its place, as the old screen does
    private fun List<WorkoutBlock>.fillAdded(blockId: Long): List<WorkoutBlock> {
        if (!prefill.fillsSets()) return this
        return map { block ->
            if (block.id != blockId) return@map block
            val index = block.rounds.lastIndex
            val round = block.rounds[index]
            block.copy(rounds = block.rounds.dropLast(1) + round.copy(sets = round.sets.mapIndexed { i, set ->
                val source = sources[block.exercises[i].exercise?.uuid] ?: return@mapIndexed set
                set.filledFrom { prefill.fillAdded(source, it, index) }
            }))
        }
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

    // A change to the workout, saved as the ongoing one straight away. Any change but a delete ends the undo of the
    // last delete, which puts its sets back as they were.
    private fun change(keepUndo: Boolean = false, reducer: WorkoutLogState.() -> WorkoutLogState) {
        setState { (if (keepUndo) this else copy(deletion = null)).reducer() }
        save()
    }

    private fun save() {
        if (state.value.exit == null) {
            store.saveOngoing(workout.withSessionBlocks(state.value.blocks))
            notifyWorkout()
        }
    }

    private fun notifyWorkout() = notification.update(notificationWorkout())

    // The workout for the notification, with a running set timer's seconds left on its set, which it shows
    private fun notificationWorkout(): ELWorkout {
        val state = state.value
        val workout = workout.withSessionBlocks(state.blocks)
        val timer = state.timer as? WorkoutTimer.SetTimer ?: return workout
        val blockIndex = state.blocks.indexOfFirst { it.id == timer.ref.blockId }
        val roundIndex = state.blocks.getOrNull(blockIndex)?.rounds?.indexOfFirst { it.id == timer.ref.roundId } ?: -1
        workout.getExerciseGroups().getOrNull(blockIndex)?.exercises?.getOrNull(timer.ref.index)?.sets?.getOrNull(roundIndex)
            ?.remainingTimeSeconds = state.timerRemainingSeconds()
        return workout
    }

    companion object {
        private const val TickMillis = 1000L
        // A countdown's −10s and +10s, AppConfig's offset for the old screen's timer
        private val TimerStepSeconds = AppConfig.configuration.defaultRestTimeOffsetSeconds
        private val MaxSets = AppConfig.configuration.maxExerciseSets
        private const val TAG = "WorkoutLogViewModel"
        private const val RepsStep = 1f
        private const val TimeStepSeconds = 5f

        /**
         * The workout to show for the one the screen was opened with: its saved copy if it's the ongoing one, with
         * everything done since it started, otherwise the new workout with the routine's targets as its values.
         */
        fun load(launched: ELWorkout, store: WorkoutLogStore): LoadedWorkout {
            // The launch intent only has the workout as it was when it started. When Android rebuilds this screen
            // after the app was killed, or it's resumed, continue from the saved copy instead.
            val saved = store.savedCopyOf(launched)
            if (saved != null) {
                saved.clearRemainingTimes()
                return LoadedWorkout(saved, resumed = true)
            }
            // A new workout starts with the routine's targets as its values
            launched.prefillRequiredMetrics()
            return LoadedWorkout(launched, resumed = false)
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

// A countdown gone: a timed set's is remembered, so the set is ticked next rather than timed again
private fun WorkoutLogState.stopped(timer: WorkoutTimer) = copy(
    timer = null,
    timedSets = if (timer is WorkoutTimer.SetTimer) timedSets + timer.ref else timedSets,
)

// The workout with some of its sets gone: anything on them goes too (the keypad, a set timer, the selection) and the
// current set, if it went, moves on to the next one not done after it that's still there
private fun WorkoutLogState.withSetsGone(remaining: List<WorkoutBlock>): WorkoutLogState {
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

/**
 * The workout the screen shows, and whether it's a saved copy being resumed.
 */
internal data class LoadedWorkout(val workout: ELWorkout, val resumed: Boolean)

// The set as the old screen's ELSet, filled in by [fill], and back. A weight filled in where there was none is the
// set's suggestion.
private fun WorkoutSet.filledFrom(fill: (ELSet) -> Unit): WorkoutSet {
    val set = toELSet()
    fill(set)
    val filled = set.toSessionSet()
    val suggested = filled.weightKg?.takeIf { it != weightKg }
    return filled.copy(suggestedKg = suggested ?: suggestedKg)
}

// The last session's sets, for the last time column
private fun PrefillSource.lastSets(): List<WorkoutSet> = lastSession?.exercise?.sets.orEmpty().map { it.toSessionSet() }
