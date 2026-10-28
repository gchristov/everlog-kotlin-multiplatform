package com.everlog.ui.components.exercisetable

import com.everlog.data.controllers.workoutsession.SetMode
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.WorkoutSet
import com.everlog.data.model.exercise.ELExercise
import com.everlog.managers.preferences.SettingsManager.WeightUnit
import com.everlog.utils.format.EmptyValue
import com.everlog.utils.format.displayWeight
import com.everlog.utils.format.formatClock
import com.everlog.utils.format.formatWeight
import com.everlog.utils.format.lastTimeText

/**
 * What the table shows on top of the sets.
 *
 * @param unitLabel KG or LB
 * @param startsTimer Whether a set's row shows a timer button in place of its tick
 */
internal data class TableView(
    val unit: WeightUnit,
    val unitLabel: String,
    val focus: CellFocus? = null,
    val selected: SetRef? = null,
    val typed: String? = null,
    val timing: Countdown? = null,
    // The set whose tick (or timer button) the keypad's main key presses
    val armed: SetRef? = null,
    val startsTimer: (SetRef) -> Boolean = { false },
    val lastTime: Map<String, List<WorkoutSet>> = emptyMap(),
    val columns: TableColumns = TableColumns(),
    // Prototype: shorter rows
    val dense: Boolean = false,
)

// The columns a table can leave out, e.g. a template's, which has targets only. SET and REPS (or TIME) always show.
internal data class TableColumns(
    val lastTime: Boolean = true,
    val weight: Boolean = true,
    val tick: Boolean = true,
)

internal data class Countdown(val ref: SetRef, val seconds: Int)

// The cell the keypad types into
internal data class CellFocus(val ref: SetRef, val field: ValueField)

internal fun countField(set: WorkoutSet) = if (set.mode == SetMode.Time) ValueField.Time else ValueField.Reps

/**
 * One exercise's set in a round, as its row shows it. [round] and [exercise] are its place, for test tags.
 */
internal data class SetRow(
    val ref: SetRef,
    val round: Int,
    val exercise: Int,
    // The first exercise's row in its round, which a divider goes above
    val startsRound: Boolean,
    val number: String?,
    val lastTime: String?,
    val countField: ValueField,
    val countText: String,
    val weightText: String,
    val done: Boolean,
    val selected: Boolean,
    val countFocused: Boolean,
    val weightFocused: Boolean,
    // A set timer is counting down in its time cell
    val timing: Boolean,
    // Lit up when the keypad's main key will complete the set
    val armed: Boolean,
    val countSuggested: Boolean,
    val weightSuggested: Boolean,
    val startsTimer: Boolean,
)

internal fun WorkoutBlock.rows(view: TableView): List<SetRow> = rounds.flatMapIndexed { r, round ->
    val firstShown = round.sets.indexOfFirst { !it.removed }
    round.sets.mapIndexedNotNull { i, set ->
        if (set.removed) return@mapIndexedNotNull null
        val ref = SetRef(id, round.id, i)
        val countdown = view.timing?.takeIf { it.ref == ref }
        val countField = countField(set)
        val weightFocused = view.focus == CellFocus(ref, ValueField.Weight)
        SetRow(
            ref = ref,
            round = r,
            exercise = i,
            startsRound = i == firstShown,
            // A superset's rows are 1a, 1b, 1c
            number = if (round.sets.size > 1) "${r + 1}${'a' + i}" else if (i == firstShown) "${r + 1}" else null,
            lastTime = view.lastTime[exercises[i].exercise?.uuid]?.getOrNull(r)?.let { lastTimeText(it, view.unit, view.unitLabel) },
            countField = countField,
            countText = countdown?.let { formatClock(it.seconds.toLong()) } ?: cellText(set, countField, ref, view),
            weightText = cellText(set, ValueField.Weight, ref, view),
            done = set.done,
            selected = view.selected == ref,
            countFocused = view.focus?.ref == ref && view.focus.field != ValueField.Weight,
            weightFocused = weightFocused,
            timing = countdown != null,
            armed = !set.done && (view.armed == ref || countdown != null),
            countSuggested = countdown == null && set.isSuggested(countField),
            weightSuggested = set.isSuggested(ValueField.Weight),
            startsTimer = view.startsTimer(ref),
        )
    }
}

// What's being typed, or else the value
private fun cellText(set: WorkoutSet, field: ValueField, ref: SetRef, view: TableView): String {
    val typing = view.typed?.takeIf { view.focus == CellFocus(ref, field) }
    return when (field) {
        ValueField.Weight -> typing ?: set.weightKg?.let { formatWeight(displayWeight(it, view.unit)) } ?: EmptyValue
        ValueField.Reps -> typing ?: set.reps?.toString() ?: EmptyValue
        ValueField.Time -> (typing?.let { typedSeconds(it) } ?: set.timeSeconds)?.let { formatClock(it.toLong()) } ?: EmptyValue
    }
}

/**
 * What tapping the table does. Every action does nothing unless overridden, for a table that can't be used.
 */
internal interface ExerciseTableActions {
    fun onCellTapped(cell: CellFocus) = Unit

    fun onTick(ref: SetRef) = Unit

    fun onStartTimer(ref: SetRef) = Unit

    fun onAddSet(blockId: Long) = Unit

    fun onDeleteSet(ref: SetRef) = Unit

    fun onOpenExercise(exercise: ELExercise, tab: ExerciseTab) = Unit

    fun onDeleteExercise(blockId: Long) = Unit
}

// Which of an exercise's details opens
internal enum class ExerciseTab {
    Info,
    History,
}
