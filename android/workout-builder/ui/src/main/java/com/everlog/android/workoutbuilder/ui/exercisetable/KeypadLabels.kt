package com.everlog.android.workoutbuilder.ui.exercisetable

import com.everlog.android.common.design.R as DesignR
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.everlog.android.common.kotlin.WeightUnit
import com.everlog.android.workoutbuilder.data.SetRef
import com.everlog.android.workoutbuilder.data.ValueField
import com.everlog.android.workoutbuilder.data.WorkoutBlock
import com.everlog.android.workoutbuilder.data.block
import com.everlog.android.workoutbuilder.ui.R
import com.everlog.android.common.kotlin.format.unitLabel

// SET 2 OF 4 · KG, with a superset's exercise: SET 1 OF 3 · 2 OF 2 · REPS
@Composable
fun focusLabel(blocks: List<WorkoutBlock>, focus: CellFocus, unit: WeightUnit): String {
    val block = blocks.block(focus.ref.blockId) ?: return ""
    val round = block.rounds.indexOfFirst { it.id == focus.ref.roundId } + 1
    val field = when (focus.field) {
        ValueField.Weight -> unitLabel(unit)
        ValueField.Reps -> stringResource(DesignR.string.workout_log_column_reps)
        ValueField.Time -> stringResource(DesignR.string.workout_log_column_time)
    }
    return if (block.exercises.size > 1) {
        stringResource(R.string.workout_log_keypad_label_group, round, block.rounds.size, focus.ref.index + 1, block.exercises.size, field)
    } else {
        stringResource(R.string.workout_log_keypad_label, round, block.rounds.size, field)
    }
}

fun exerciseName(blocks: List<WorkoutBlock>, ref: SetRef): String =
    blocks.block(ref.blockId)?.exercises?.getOrNull(ref.index)?.exercise?.name.orEmpty()
