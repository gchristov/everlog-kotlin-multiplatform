package com.everlog.ui.activities.templateeditor

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.kotlin.Load
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.common.mvvm.CommonViewModel
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.exercise.data.model.library.toELExercise
import com.everlog.config.AppConfig
import com.everlog.data.controllers.templatesession.LoadTemplateUseCase
import com.everlog.data.controllers.templatesession.SaveTemplateUseCase
import com.everlog.data.controllers.templatesession.TemplateNotFoundException
import com.everlog.data.controllers.templatesession.addTemplateGroups
import com.everlog.data.controllers.templatesession.groupExercises
import com.everlog.data.controllers.templatesession.setExerciseMode
import com.everlog.data.controllers.templatesession.ungroupExercises
import com.everlog.data.controllers.templatesession.withTimeForAllSets
import com.everlog.data.controllers.workoutsession.ExerciseRef
import com.everlog.data.controllers.workoutsession.SessionIds
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.addRound
import com.everlog.data.controllers.workoutsession.block
import com.everlog.data.controllers.workoutsession.deleteSet
import com.everlog.data.controllers.workoutsession.moveBlock
import com.everlog.data.controllers.workoutsession.refs
import com.everlog.data.controllers.workoutsession.restore
import com.everlog.data.controllers.workoutsession.set
import com.everlog.data.controllers.workoutsession.setValue
import com.everlog.data.controllers.workoutsession.withExercises
import com.everlog.ui.components.exercisetable.CellFocus
import com.everlog.ui.components.exercisetable.parse
import com.everlog.ui.components.exercisetable.steppedValue
import com.everlog.ui.components.exercisetable.typedAfterBackspace
import com.everlog.ui.components.exercisetable.typedAfterKey
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Builds a new workout template or edits one, as a table typed into with the workout screen's keypad. Nothing is
 * saved until Save; leaving with changes asks first.
 *
 * @param uuid The template to edit, read through DataSync, or null for a new one
 * @param defaultName A new template's name, and the name of one without
 */
internal class TemplateEditorViewModel(
    dispatcher: CoroutineDispatcher,
    private val uuid: String?,
    private val defaultName: String,
    private val loadTemplate: LoadTemplateUseCase,
    private val saveTemplate: SaveTemplateUseCase,
    private val analytics: TemplateEditorAnalytics,
    private val ownExerciseUpdates: () -> Flow<List<LibraryExercise>>,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ids: SessionIds = SessionIds(),
) : CommonViewModel<TemplateEditorState>(
    dispatcher,
    TemplateEditorState(isNew = uuid == null, load = if (uuid == null) TemplateLoad.Ready else TemplateLoad.Loading),
) {

    private var loading: Job? = null

    init {
        if (uuid == null) {
            val routine = ELRoutine(uuid = UUID.randomUUID().toString(), name = defaultName, createdDate = clock())
            setState { opened(routine, ids, defaultName) }
        } else {
            load(uuid)
        }
        followOwnExercises()
    }

    // The user's own exercises follow the cache while the editor is open, so one renamed from its details shows
    // straight away. Not an edit of the template's, so leaving doesn't ask about it.
    private fun followOwnExercises() {
        launchCoroutine {
            ownExerciseUpdates().collect { own ->
                val current = own.associate { it.uuid to it.toELExercise() }
                setState { copy(blocks = blocks.withExercises(current)) }
            }
        }
    }

    fun onResume() = analytics.screenViewed()

    // The template as it is when it opens. Changes made elsewhere while it's open don't replace the edits.
    private fun load(uuid: String) {
        loading?.cancel()
        setState { copy(load = TemplateLoad.Loading) }
        loading = launchCoroutine {
            when (val load = loadTemplate(uuid).first { it != Load.Loading }) {
                is Load.Ready -> setState { opened(load.value, ids, defaultName) }
                is Load.Unavailable -> setState {
                    copy(load = if (load.error is TemplateNotFoundException) TemplateLoad.NotFound else TemplateLoad.Failed)
                }
                Load.Loading -> error("Filtered out")
            }
        }
    }

    fun onRetry() {
        load(uuid ?: return)
    }

    fun onCellTapped(cell: CellFocus) {
        finishTyping()
        setState { focusOn(cell) }
    }

    // On the set last typed into, or the first
    fun onShowKeypad() {
        val state = state.value
        if (state.focus != null) return
        val ref = state.selected?.takeIf { state.blocks.set(it)?.removed == false } ?: state.blocks.refs().firstOrNull() ?: return
        setState { focusOn(countCell(ref)) }
    }

    fun onHideKeypad() {
        finishTyping()
        setState { focusOn(null) }
    }

    fun onBack() {
        when {
            state.value.focus != null -> onHideKeypad()
            else -> onClose()
        }
    }

    fun onDigit(digit: Char) = type(digit)

    fun onDecimal() = type('.')

    private fun type(key: Char) {
        val state = state.value
        val cell = state.focus ?: return
        enter(cell, typedAfterKey(state.typed, cell.field, key) ?: return)
    }

    fun onBackspace() {
        val state = state.value
        val cell = state.focus ?: return
        val set = state.blocks.set(cell.ref) ?: return
        enter(cell, typedAfterBackspace(state.typed, set, cell.field, NoWeights))
    }

    private fun enter(cell: CellFocus, text: String) =
        change { copy(blocks = blocks.setValue(cell.ref, cell.field, parse(cell.field, text)), typed = text, reveal = reveal + 1) }

    fun onStep(direction: Int) {
        finishTyping()
        val state = state.value
        val cell = state.focus ?: return
        val set = state.blocks.set(cell.ref) ?: return
        val value = steppedValue(set, cell.field, NoWeights, weightStep = 0f, direction)
        change { copy(blocks = blocks.setValue(cell.ref, cell.field, value), reveal = reveal + 1) }
    }

    // Moving on keeps what was typed: there's nothing to save until Save
    private fun finishTyping() = setState { copy(typed = null) }

    fun onMainKey() {
        finishTyping()
        val next = state.value.nextSet() ?: return onHideKeypad()
        setState { focusOn(countCell(next)) }
    }

    // Reps or time is the exercise's, so it switches all of its sets
    fun onModeSelected(mode: SetMode) {
        finishTyping()
        val ref = state.value.focus?.ref ?: return
        change {
            val switched = copy(blocks = blocks.setExerciseMode(ref.blockId, ref.index, mode))
            switched.focusOn(switched.countCell(ref))
        }
    }

    fun onTimeForAllSets() {
        finishTyping()
        val state = state.value
        val ref = state.focus?.ref ?: return
        val seconds = state.timeForAllSets() ?: return
        change { copy(blocks = blocks.withTimeForAllSets(ref.blockId, ref.index, seconds)) }
        analytics.timeForAllSets(seconds)
    }

    fun onAddSet(blockId: Long) {
        val block = state.value.blocks.block(blockId) ?: return
        if (block.rounds.size >= MaxSets) {
            setState { copy(message = TemplateEditorMessage.TooManySets(MaxSets)) }
            return
        }
        change { copy(blocks = blocks.addRound(blockId, ids)) }
        analytics.setAdded()
    }

    fun onDeleteSet(ref: SetRef) {
        finishTyping()
        val (blocks, deleted) = state.value.blocks.deleteSet(ref) ?: return
        change(keepUndo = true) { copy(blocks = blocks, deletion = deleted).withoutGoneSets() }
        analytics.setDeleted()
    }

    fun onUndoDelete() {
        val deleted = state.value.deletion ?: return
        change { copy(blocks = blocks.restore(deleted)) }
    }

    fun onUndoGone() = setState { copy(deletion = null) }

    fun onMessageShown() = setState { copy(message = null) }

    fun onMoveExercise(from: Int, to: Int) = change { copy(blocks = blocks.moveBlock(from, to)) }

    // A drag closes the keypad, as the exercises shrink to their names while it lasts
    fun onDragStarted() {
        finishTyping()
        setState { focusOn(null) }
    }

    // Makes an exercise, or a group, one group with the exercise after it: a superset of two, a circuit of more, or
    // the group's own type when it can take them all
    fun onJoinWithNext(blockId: Long) {
        finishTyping()
        val blocks = state.value.blocks
        val at = blocks.indexOfFirst { it.id == blockId }
        val first = blocks.getOrNull(at) ?: return
        val next = blocks.getOrNull(at + 1) ?: return
        val exercises = listOf(first, next).flatMap { block -> block.exercises.indices.map { ExerciseRef(block.id, it) } }
        val type = first.setType?.takeIf { first.exercises.size > 1 && it.canBeSelected(exercises.size) }
            ?: if (exercises.size == 2) ELSetType.SUPER else ELSetType.CIRCUIT
        change { focusOn(null).copy(blocks = blocks.groupExercises(exercises, type, ids)).withoutGoneSets() }
        analytics.setTypeChanged(type)
    }

    // Every exercise of a group on its own again, in the group's order
    fun onSplitGroup(blockId: Long) {
        finishTyping()
        val block = state.value.blocks.block(blockId)?.takeIf { it.exercises.size > 1 } ?: return
        val out = block.exercises.indices.drop(1).map { ExerciseRef(blockId, it) }
        change { focusOn(null).copy(blocks = blocks.ungroupExercises(out, ids)).withoutGoneSets() }
        analytics.setTypeChanged(ELSetType.SINGLE)
    }

    fun onExercisesPicked(groups: List<ELExerciseGroup>) {
        if (groups.isEmpty()) return
        change { copy(blocks = blocks.addTemplateGroups(groups, ids)) }
        analytics.exercisesAdded()
    }

    fun onNameTapped() {
        finishTyping()
        setState { focusOn(null).copy(dialog = TemplateEditorDialog.Name) }
    }

    // A blank name keeps the one it had
    fun onNameSaved(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed == state.value.name) return setState { copy(dialog = null) }
        change { copy(name = trimmed, dialog = null) }
    }

    fun onRestTapped() {
        finishTyping()
        setState { focusOn(null).copy(dialog = TemplateEditorDialog.Rest) }
    }

    // Never below a step, unless it was saved with less (the old editor could turn rest off), which − then keeps
    fun onRestStep(direction: Int) {
        val current = state.value.restSeconds
        val seconds = (current + direction * RestStepSeconds).coerceAtLeast(minOf(RestStepSeconds, current))
        change { copy(restSeconds = seconds) }
        analytics.restChanged(seconds)
    }

    fun onDialogDismissed() = setState { copy(dialog = null) }

    fun onSave() {
        finishTyping()
        val state = state.value
        if (state.saving) return
        // A new template saves even untouched, as the old editor's did; an existing one only when it differs
        if (!state.hasChanges() && !state.isNew) return setState { copy(dialog = null, exit = TemplateEditorExit.Closed) }
        val edited = state.edited() ?: return
        setState { copy(saving = true) }
        launchCoroutine {
            saveTemplate(edited).fold(
                { setState { copy(saving = false, dialog = null, message = TemplateEditorMessage.SaveFailed) } },
                {
                    analytics.templateSaved(state.isNew)
                    setState { copy(saving = false, dialog = null, exit = TemplateEditorExit.Saved(edited, state.isNew)) }
                },
            )
        }
    }

    // Close or Back. Changes are only lost once the user says so.
    fun onClose() {
        finishTyping()
        setState { if (hasChanges()) copy(dialog = TemplateEditorDialog.Leave) else copy(exit = TemplateEditorExit.Closed) }
    }

    fun onDiscard() = setState { copy(dialog = null, exit = TemplateEditorExit.Closed) }

    // Every edit goes through here, and ends the undo of a delete unless it's the delete itself
    private fun change(keepUndo: Boolean = false, reducer: TemplateEditorState.() -> TemplateEditorState) = setState {
        reducer().let { if (keepUndo) it else it.copy(deletion = null) }
    }

    companion object {
        private val MaxSets = AppConfig.configuration.maxExerciseSets
        private val RestStepSeconds = AppConfig.configuration.defaultRestTimeOffsetSeconds
        // Templates hold reps and times only, so the unit never applies
        private val NoWeights = WeightUnit.KILOGRAM
    }
}
