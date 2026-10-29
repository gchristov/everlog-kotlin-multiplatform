package com.everlog.ui.activities.templateeditor

import com.everlog.data.controllers.workoutsession.DeletedSets
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.model.ELRoutine
import com.everlog.ui.components.exercisetable.CellFocus

/**
 * The template editor's state. [routine] is the template as loaded (or new), which saving copies [name],
 * [restSeconds] and [blocks] onto.
 */
internal data class TemplateEditorState(
    val isNew: Boolean,
    val load: TemplateLoad,
    val routine: ELRoutine? = null,
    val name: String = "",
    val restSeconds: Int = 0,
    val blocks: List<WorkoutBlock> = emptyList(),
    // The template as it opened, which leaving compares with to ask about changes
    val opened: TemplateContent? = null,
    // The cell the keypad types into; null while it's closed
    val focus: CellFocus? = null,
    // The set last typed into, which the keyboard button opens on again
    val selected: SetRef? = null,
    // What's been typed into the focused cell. Null until the first key, which replaces its value.
    val typed: String? = null,
    // Goes up with every key, to bring the edited cell back into view
    val reveal: Int = 0,
    val dialog: TemplateEditorDialog? = null,
    val message: TemplateEditorMessage? = null,
    val deletion: DeletedSets? = null,
    val saving: Boolean = false,
    val exit: TemplateEditorExit? = null,
)

/**
 * What saving a template writes, less the exercises' stored copies, so a rename isn't a change of the template's.
 */
internal data class TemplateContent(
    val name: String,
    val restSeconds: Int,
    val groups: List<Map<String, Any?>>,
)

internal enum class TemplateLoad {
    Loading,
    Ready,
    // Couldn't be read (offline before it was ever synced); trying again may work
    Failed,
    // Deleted, e.g. on another phone
    NotFound,
}

// The keypad's main key: on to the next set's reps or time, or closing the keypad after the last
internal enum class TemplateMainKey {
    NextSet,
    Done,
}

internal sealed interface TemplateEditorDialog {
    data object Name : TemplateEditorDialog

    data object Rest : TemplateEditorDialog

    // Leaving with changes
    data object Leave : TemplateEditorDialog
}

internal sealed interface TemplateEditorMessage {
    data class TooManySets(val max: Int) : TemplateEditorMessage

    data object SaveFailed : TemplateEditorMessage
}

internal sealed interface TemplateEditorExit {
    data class Saved(val routine: ELRoutine, val isNew: Boolean) : TemplateEditorExit

    data object Closed : TemplateEditorExit
}
