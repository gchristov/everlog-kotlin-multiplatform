package com.everlog.ui.activities.templateeditor

import com.everlog.config.AppConfig
import com.everlog.data.controllers.templatesession.LoadTemplateUseCase
import com.everlog.data.controllers.templatesession.SaveTemplateUseCase
import com.everlog.data.controllers.templatesession.TemplateNotFoundException
import com.everlog.data.controllers.templatesession.addTemplateGroups
import com.everlog.data.controllers.templatesession.setExerciseMode
import com.everlog.data.controllers.templatesession.withTemplateBlocks
import com.everlog.data.controllers.templatesession.withTimeForAllSets
import com.everlog.data.controllers.workoutsession.SessionIds
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.addRound
import com.everlog.data.controllers.workoutsession.block
import com.everlog.data.controllers.workoutsession.deleteBlock
import com.everlog.data.controllers.workoutsession.deleteSet
import com.everlog.data.controllers.workoutsession.refs
import com.everlog.data.controllers.workoutsession.restore
import com.everlog.data.controllers.workoutsession.set
import com.everlog.data.controllers.workoutsession.setValue
import com.everlog.data.model.ELRoutine
import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.data.sync.Load
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.ui.components.exercisetable.CellFocus
import com.everlog.ui.components.exercisetable.parse
import com.everlog.ui.components.exercisetable.steppedValue
import com.everlog.ui.components.exercisetable.typedAfterBackspace
import com.everlog.ui.components.exercisetable.typedAfterKey
import com.everlog.ui.mvvm.CommonViewModel
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
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

    fun onDeleteExercise(blockId: Long) = setState { copy(dialog = TemplateEditorDialog.DeleteExercise(blockId)) }

    fun onDeleteExerciseConfirmed(blockId: Long) {
        finishTyping()
        change { copy(blocks = blocks.deleteBlock(blockId), dialog = null).withoutGoneSets() }
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

    fun onRestStep(direction: Int) {
        val seconds = (state.value.restSeconds + direction * RestStepSeconds).coerceAtLeast(RestStepSeconds)
        change { copy(restSeconds = seconds) }
        analytics.restChanged(seconds)
    }

    fun onDialogDismissed() = setState { copy(dialog = null) }

    fun onSave() {
        finishTyping()
        val state = state.value
        if (state.saving) return
        val routine = state.routine ?: return
        if (!state.changed && !state.isNew) return setState { copy(dialog = null, exit = TemplateEditorExit.Closed) }
        val edited = routine.withTemplateBlocks(state.blocks, state.restSeconds).apply { name = state.name }
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
        setState { if (changed) copy(dialog = TemplateEditorDialog.Leave) else copy(exit = TemplateEditorExit.Closed) }
    }

    fun onDiscard() = setState { copy(dialog = null, exit = TemplateEditorExit.Closed) }

    // Every edit goes through here, and ends the undo of a delete unless it's the delete itself
    private fun change(keepUndo: Boolean = false, reducer: TemplateEditorState.() -> TemplateEditorState) = setState {
        reducer().copy(changed = true).let { if (keepUndo) it else it.copy(deletion = null) }
    }

    companion object {
        private val MaxSets = AppConfig.configuration.maxExerciseSets
        private val RestStepSeconds = AppConfig.configuration.defaultRestTimeOffsetSeconds
        // Templates hold reps and times only, so the unit never applies
        private val NoWeights = WeightUnit.KILOGRAM
    }
}
