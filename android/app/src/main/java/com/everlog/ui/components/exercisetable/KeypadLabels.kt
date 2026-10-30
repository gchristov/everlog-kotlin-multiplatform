package com.everlog.ui.components.exercisetable

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.everlog.R
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.data.controllers.workoutsession.SetRef
import com.everlog.data.controllers.workoutsession.ValueField
import com.everlog.data.controllers.workoutsession.WorkoutBlock
import com.everlog.data.controllers.workoutsession.block
import com.everlog.utils.format.unitLabel

// SET 2 OF 4 · KG, with a superset's exercise: SET 1 OF 3 · 2 OF 2 · REPS
@Composable
internal fun focusLabel(blocks: List<WorkoutBlock>, focus: CellFocus, unit: WeightUnit): String {
    val block = blocks.block(focus.ref.blockId) ?: return ""
    val round = block.rounds.indexOfFirst { it.id == focus.ref.roundId } + 1
    val field = when (focus.field) {
        ValueField.Weight -> unitLabel(unit)
        ValueField.Reps -> stringResource(R.string.workout_log_column_reps)
        ValueField.Time -> stringResource(R.string.workout_log_column_time)
    }
    return if (block.exercises.size > 1) {
        stringResource(R.string.workout_log_keypad_label_group, round, block.rounds.size, focus.ref.index + 1, block.exercises.size, field)
    } else {
        stringResource(R.string.workout_log_keypad_label, round, block.rounds.size, field)
    }
}

internal fun exerciseName(blocks: List<WorkoutBlock>, ref: SetRef): String =
    blocks.block(ref.blockId)?.exercises?.getOrNull(ref.index)?.exercise?.name.orEmpty()
