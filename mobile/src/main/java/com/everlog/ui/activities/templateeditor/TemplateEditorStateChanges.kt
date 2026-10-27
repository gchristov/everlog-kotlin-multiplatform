package com.everlog.ui.activities.templateeditor

import com.everlog.data.controllers.templatesession.templateRestSeconds
import com.everlog.data.controllers.templatesession.toTemplateBlocks
import com.everlog.data.controllers.workoutsession.SessionIds
import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.block
import com.everlog.data.controllers.workoutsession.refs
import com.everlog.data.controllers.workoutsession.set
import com.everlog.data.model.ELRoutine
import com.everlog.ui.components.exercisetable.CellFocus
import com.everlog.ui.components.exercisetable.countField

internal fun TemplateEditorState.opened(routine: ELRoutine, ids: SessionIds, defaultName: String) = copy(
    load = TemplateLoad.Ready,
    routine = routine,
    name = routine.name?.takeIf { it.isNotBlank() } ?: defaultName,
    restSeconds = routine.templateRestSeconds(),
    blocks = routine.toTemplateBlocks(ids),
)

// Closing the keypad (a null cell) keeps the selection, so the keyboard button opens on it again
internal fun TemplateEditorState.focusOn(cell: CellFocus?) = copy(focus = cell, typed = null, selected = cell?.ref ?: selected)

internal fun TemplateEditorState.countCell(ref: SetRef): CellFocus? = blocks.set(ref)?.let { CellFocus(ref, countField(it)) }

// The set after the focused one, in the table's order
internal fun TemplateEditorState.nextSet(): SetRef? {
    val refs = blocks.refs()
    val at = refs.indexOf(focus?.ref ?: return null)
    return refs.getOrNull(at + 1)
}

internal fun TemplateEditorState.mainKey(): TemplateMainKey? = when {
    focus == null -> null
    nextSet() != null -> TemplateMainKey.NextSet
    else -> TemplateMainKey.Done
}

/**
 * The focused time, when it differs from another set of the same exercise, so the keypad can offer it for every set.
 */
internal fun TemplateEditorState.timeForAllSets(): Int? {
    val cell = focus?.takeIf { it.field == ValueField.Time } ?: return null
    val seconds = blocks.set(cell.ref)?.timeSeconds ?: return null
    val block = blocks.block(cell.ref.blockId) ?: return null
    val others = block.exerciseSets(cell.ref.index)
    return seconds.takeIf { others.size > 1 && others.any { it.mode != SetMode.Time || it.timeSeconds != seconds } }
}

private fun WorkoutBlock.exerciseSets(index: Int) = rounds.map { it.sets[index] }.filterNot { it.removed }

// After sets or exercises go: a cell or selection that went with them goes too
internal fun TemplateEditorState.withoutGoneSets() = copy(
    focus = focus?.takeIf { blocks.set(it.ref)?.removed == false },
    selected = selected?.takeIf { blocks.set(it)?.removed == false },
    typed = typed?.takeIf { focus?.let { blocks.set(it.ref)?.removed == false } == true },
)
