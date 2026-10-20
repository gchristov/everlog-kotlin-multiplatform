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
 * The ongoing workout as a table, typed into with a docked keypad. The workout is saved as the ongoing one after
 * every change, so it can be resumed, and finishing saves it to Firestore and checks it off the plan.
 *
 * @param workout The workout as launched or resumed (see [load]). The state holds its exercises; everything else
 * comes from here.
 * @param resumed Whether [workout] is a saved copy being resumed, whose sets were filled in from history already
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
    private var showing = false

    // By exercise uuid, once the user's history has loaded
    private var sources: Map<String, PrefillSource> = emptyMap()

    init {
        save()
        // A resumed workout was filled in when it started, and the user may have cleared values since
        loadHistory(state.value.blocks, fill = !resumed)
    }

    // A setting such as the weight unit may have changed while the screen was away, and the notification shows it too
    fun onResume() {
        showing = true
        analytics.screenViewed()
        setState { withSettings(settings).copy(now = clock()) }
        notifyWorkout()
        keepTicking()
    }

    // The time stops while the screen's away, unless a countdown is running, so a rest still sounds with the phone locked
    fun onPause() {
        showing = false
    }

    /**
     * Moves the time on, and ends a countdown that has run out, with a sound. A timed set is left to be ticked.
     */
    fun tick() {
        setState { copy(now = clock()) }
        val timer = state.value.timer ?: return
        if (clock() < timer.endsAt) {
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

    fun onCellTapped(cell: CellFocus) {
        logTypedValue()
        setState { focusOn(cell) }
    }

    fun onShowKeypad() = setState { (selected ?: current)?.let { focusOn(countCell(it)) } ?: this }

    fun onHideKeypad() {
        logTypedValue()
        setState { focusOn(null) }
    }

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

    fun onStep(direction: Int) {
        logTypedValue()
        val state = state.value
        val cell = state.focus ?: return
        val set = state.blocks.set(cell.ref) ?: return
        val (value, step) = when (cell.field) {
            ValueField.Weight -> (set.weightKg?.let { displayWeight(it, state.unit) } ?: 0f) to state.weightStep
            ValueField.Reps -> (set.reps ?: 0).toFloat() to RepsStep
            ValueField.Time -> (set.timeSeconds ?: 0).toFloat() to TimeStepSeconds
        }
        val stepped = (value + direction * step).coerceAtLeast(0f)
        change { withValue(cell, stepped).copy(typed = null, reveal = reveal + 1) }
        analytics.valueChanged(cell.field, stepped)
    }

    // Typing reps or time carries on in the other
    fun onModeSelected(mode: SetMode) {
        logTypedValue()
        val cell = state.value.focus ?: return
        change {
            val switched = copy(blocks = blocks.setMode(cell.ref, mode))
            if (cell.field == ValueField.Weight) switched else switched.focusOn(CellFocus(cell.ref, if (mode == SetMode.Time) ValueField.Time else ValueField.Reps))
        }
    }

    fun onMainKey() {
        logTypedValue()
        val cell = state.value.focus ?: return
        when (state.value.mainKey) {
            KeypadMainKey.Next -> setState { focusOn(CellFocus(cell.ref, ValueField.Weight)) }
            KeypadMainKey.Done -> setState { focusOn(null).copy(selected = current ?: selected) }
            KeypadMainKey.CompleteSet -> {
                complete(cell.ref)
                setState { focusOn(current?.let { countCell(it) }) }
            }
            KeypadMainKey.StartTimer -> onStartSetTimer(cell.ref)
            null -> Unit
        }
    }

    // The closed keypad's main action: a timed set's timer starts first, then the set completes
    fun onCompleteTarget() {
        logTypedValue()
        val state = state.value
        val target = state.target ?: return
        if (state.targetStartsTimer) onStartSetTimer(target) else complete(target)
    }

    // Without a time there's nothing to count down, so the keypad opens on it instead
    fun onStartSetTimer(ref: SetRef) {
        logTypedValue()
        startSetTimer(ref, fromNotification = false)
    }

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
     * A rest's change carries on to every rest after it, a set timer's to the set's time.
     */
    fun onTimerStep(direction: Int) {
        val timer = state.value.timer ?: return
        val offset = direction * TimerStepSeconds
        val total = (timer.totalSeconds + offset).coerceAtLeast(0)
        when (timer) {
            // Taken down to nothing, it only ends this rest: the ones after keep theirs
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

    // Skip on a rest, Stop on a set timer: no sound, and the set is left to be ticked
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

    fun onScreenStarted() = notification.start(notificationWorkout())

    fun onScreenClosed() = notification.stop()

    // The notification's buttons. [position] is the set it shows, in the workout it was last sent.

    fun onNotificationCompleteSet(position: ELWorkoutState) {
        setAt(position)?.let { complete(it, fromNotification = true) }
    }

    fun onNotificationWeight(position: ELWorkoutState, direction: Int) {
        val ref = setAt(position) ?: return
        val set = state.value.blocks.set(ref) ?: return
        val weight = set.weightKg?.let { displayWeight(it, state.value.unit) } ?: 0f
        val stepped = (weight + direction * weightStep).coerceAtLeast(0f)
        change { withValue(CellFocus(ref, ValueField.Weight), stepped) }
        analytics.valueChanged(ValueField.Weight, stepped)
    }

    fun onNotificationReps(position: ELWorkoutState, direction: Int) {
        val ref = setAt(position) ?: return
        val reps = state.value.blocks.set(ref)?.reps ?: 0
        val stepped = (reps + direction * RepsStep).coerceAtLeast(0f)
        change { withValue(CellFocus(ref, ValueField.Reps), stepped) }
        analytics.valueChanged(ValueField.Reps, stepped)
    }

    fun onNotificationSetTimer(position: ELWorkoutState, start: Boolean) {
        val ref = setAt(position) ?: return
        if (start) startSetTimer(ref, fromNotification = true) else if (state.value.isTiming(ref)) endTimer(fromNotification = true)
    }

    fun onNotificationRestStop() {
        if (state.value.timer is WorkoutTimer.Rest) endTimer(fromNotification = true)
    }

    // The notification counts an exercise's place among those with a set in that round, skipping removed ones
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

    // Logged once the keypad moves on from the cell, not at every key, so every event that does calls this first.
    // Logged as typed, in the user's unit, rather than converted back from kg.
    private fun logTypedValue() {
        val state = state.value
        val cell = state.focus ?: return
        val typed = state.typed ?: return
        analytics.valueChanged(cell.field, parse(cell.field, typed))
        setState { copy(typed = null) }
    }

    fun onTick(ref: SetRef) {
        logTypedValue()
        setState { if (focus != null) focusOn(countCell(ref)) else copy(selected = ref) }
        val set = state.value.blocks.set(ref) ?: return
        if (set.done) {
            change { copy(blocks = blocks.uncompleteSet(ref), current = current ?: ref) }
            analytics.setEdited(state.value.blocks.block(ref.blockId)?.setType, set.mode == SetMode.Time)
        } else {
            complete(ref)
        }
    }

    private fun complete(ref: SetRef, fromNotification: Boolean = false) {
        val before = state.value
        val set = before.blocks.set(ref) ?: return
        if (set.done) return
        val completion = before.blocks.completeSet(ref, before.current, clock())
        // A rest replaces any countdown running, and completing another set ends a running set timer
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

    fun onAddSet(blockId: Long) {
        val block = state.value.blocks.block(blockId) ?: return
        if (block.rounds.size >= MaxSets) {
            setState { copy(message = WorkoutLogMessage.TooManySets(MaxSets)) }
            return
        }
        val blocks = state.value.blocks.addRound(blockId, ids).fillAdded(blockId)
        // When every set was done, the new one is current
        change { copy(blocks = blocks, current = current ?: blocks.nextOpenAfter(null)).followCurrent() }
        analytics.setAdded()
    }

    fun onDeleteSet(ref: SetRef) {
        logTypedValue()
        val before = state.value
        val (blocks, deleted) = before.blocks.deleteSet(ref) ?: return
        change(keepUndo = true) { withSetsGone(blocks).copy(deletion = Deletion(deleted, before.current)) }
        analytics.setDeleted()
    }

    fun onUndoDelete() {
        val deletion = state.value.deletion ?: return
        change { copy(blocks = blocks.restore(deletion.sets), current = deletion.current ?: current) }
    }

    fun onUndoGone() = setState { copy(deletion = null) }

    fun onMessageShown() = setState { copy(message = null) }

    fun onDeleteExercise(blockId: Long) = setState { copy(dialog = WorkoutLogDialog.DeleteExercise(blockId)) }

    fun onDeleteExerciseConfirmed(blockId: Long) {
        logTypedValue()
        change {
            val remaining = withSetsGone(blocks.deleteBlock(blockId))
            // No rest without exercises to rest between, nor anything left to edit
            val any = remaining.blocks.isNotEmpty()
            remaining.copy(timer = remaining.timer.takeIf { any }, editing = editing && any, dialog = null)
        }
    }

    // Renamed in the library or by the user, for example
    fun onExerciseChanged(exercise: ELExercise) {
        val uuid = exercise.uuid ?: return
        if (state.value.blocks.none { block -> block.exercises.any { it.exercise?.uuid == uuid } }) return
        change {
            copy(blocks = blocks.map { block ->
                block.copy(exercises = block.exercises.map { if (it.exercise?.uuid == uuid) it.copy(exercise = exercise) else it })
            })
        }
    }

    fun onEditWorkout() {
        logTypedValue()
        setState { focusOn(null).copy(editing = true) }
    }

    fun onEditDone() = setState { copy(editing = false) }

    fun onMoveExercise(from: Int, to: Int) = change { copy(blocks = blocks.moveBlock(from, to)) }

    fun onRestTimerSettings() = setState { copy(dialog = WorkoutLogDialog.RestTimer) }

    // A rest already running keeps its time
    fun onRestStep(direction: Int) {
        val seconds = (state.value.restSeconds + direction * TimerStepSeconds).coerceAtLeast(TimerStepSeconds)
        change { copy(blocks = blocks.withRestSeconds(seconds)) }
        analytics.restChanged(seconds)
    }

    fun onExercisesPicked(groups: List<ELExerciseGroup>) {
        if (groups.isEmpty()) return
        val before = state.value.blocks
        change {
            val blocks = blocks.addGroups(groups, ids)
            // In an empty workout, or with every set done, the first new set becomes current
            copy(blocks = blocks, current = current ?: blocks.nextOpenAfter(this.blocks.refs().lastOrNull())).followCurrent()
        }
        analytics.exercisesAdded()
        // Only the new exercises, so values cleared in the others aren't filled in again
        loadHistory(state.value.blocks.filter { block -> before.none { it.id == block.id } }, fill = true)
    }

    /**
     * Loads each exercise's history, for its last time and for sets added later. With [fill], and when the muscle
     * goal fills sets in, it fills their empty values too.
     */
    private fun loadHistory(blocks: List<WorkoutBlock>, fill: Boolean) {
        val exercises = blocks.flatMap { block -> block.exercises.mapNotNull { it.exercise } }.distinctBy { it.uuid }
        if (exercises.isEmpty()) return
        launchCoroutine {
            val found = try {
                prefill.sources(exercises)
            } catch (e: Exception) {
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

    private fun WorkoutBlock.filled(): WorkoutBlock = copy(rounds = rounds.mapIndexed { index, round ->
        round.copy(sets = round.sets.mapIndexed { i, set ->
            val source = sources[exercises[i].exercise?.uuid]
            if (set.removed || set.done || source == null) set else set.filledFrom { prefill.fill(source, it, index) }
        })
    })

    // The set just added takes the last session's set at its place, when there was one
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

    // Unticked sets are saved as they are, those with reps or a time as done, so it asks first
    fun onFinish() {
        logTypedValue()
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

    // Close or Back. With sets done, the prompt offers Finish instead.
    fun onClose() {
        logTypedValue()
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

    // Undo puts the deleted block back as it was, so any other change ends it
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

    // The notification shows a running set timer's seconds left from its set
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
        private val TimerStepSeconds = AppConfig.configuration.defaultRestTimeOffsetSeconds
        private val MaxSets = AppConfig.configuration.maxExerciseSets
        private const val TAG = "WorkoutLogViewModel"
        private const val RepsStep = 1f
        private const val TimeStepSeconds = 5f

        /**
         * The launch intent only has the workout as it was when it started. When it's resumed, or Android rebuilds
         * the screen after the app was killed, the saved copy has everything done since.
         */
        fun load(launched: ELWorkout, store: WorkoutLogStore): LoadedWorkout {
            val saved = store.savedCopyOf(launched)
            if (saved != null) {
                saved.clearRemainingTimes()
                return LoadedWorkout(saved, resumed = true)
            }
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

        // The notification picks the next set the same way
        private fun List<WorkoutBlock>.resumeAt(): SetRef? {
            val last = refs().filter { set(it)?.done == true }.maxByOrNull { set(it)?.completedDate ?: 0 }
            return nextOpenAfter(last)
        }

    }
}

// The first key after focusing replaces the value
private fun WorkoutLogState.focusOn(cell: CellFocus?) = copy(focus = cell, typed = null, selected = cell?.ref ?: selected)

private fun WorkoutLogState.countCell(ref: SetRef): CellFocus? = blocks.set(ref)?.let { CellFocus(ref, countField(it)) }

// A weight is typed in the user's unit and stored in kg
private fun WorkoutLogState.withValue(cell: CellFocus, value: Float?): WorkoutLogState {
    val stored = if (cell.field == ValueField.Weight) value?.let { storedWeight(it, unit) } else value
    return copy(blocks = blocks.setValue(cell.ref, cell.field, stored))
}

// Once the selected set is done, the selection (and the open keypad) moves on to the current set. So does an empty
// selection, e.g. in an empty workout that just got its first exercise.
private fun WorkoutLogState.followCurrent(): WorkoutLogState {
    val next = current ?: return this
    val selectedSet = selected?.let { blocks.set(it) }
    if (selectedSet != null && !selectedSet.done) return this
    return if (focus != null) focusOn(countCell(next)) else copy(selected = next)
}

// A timed set whose timer has run is ticked next rather than timed again
private fun WorkoutLogState.stopped(timer: WorkoutTimer) = copy(
    timer = null,
    timedSets = if (timer is WorkoutTimer.SetTimer) timedSets + timer.ref else timedSets,
)

// Anything on the sets that went goes too (the keypad, a set timer, the selection), and the current set moves on to
// the next one not done that's still there
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

internal data class LoadedWorkout(val workout: ELWorkout, val resumed: Boolean)

// The prefill controller fills an ELSet. A weight it fills in is the set's suggestion.
private fun WorkoutSet.filledFrom(fill: (ELSet) -> Unit): WorkoutSet {
    val set = toELSet()
    fill(set)
    val filled = set.toSessionSet()
    val suggested = filled.weightKg?.takeIf { it != weightKg }
    return filled.copy(suggestedKg = suggested ?: suggestedKg)
}

private fun PrefillSource.lastSets(): List<WorkoutSet> = lastSession?.exercise?.sets.orEmpty().map { it.toSessionSet() }
