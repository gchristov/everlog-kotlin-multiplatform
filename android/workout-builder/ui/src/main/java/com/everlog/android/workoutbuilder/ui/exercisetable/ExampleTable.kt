package com.everlog.android.workoutbuilder.ui.exercisetable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.everlog.android.common.compose.elements.AppExample
import com.everlog.android.common.compose.elements.AppExampleLabel
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.workoutbuilder.data.WorkoutBlock

/**
 * A faded example of what the table becomes, under an EXAMPLE label, which can't be touched: an empty screen's
 * placeholder.
 */
@Composable
fun ExampleTable(blocks: List<WorkoutBlock>, view: TableView, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        AppExampleLabel(modifier = Modifier.padding(horizontal = Theme.spacing.large, vertical = Theme.spacing.small))
        AppExample(modifier = Modifier.fillMaxSize()) {
            ExerciseTable(
                blocks = blocks,
                view = view,
                reveal = 0,
                contentPadding = contentPadding,
                actions = object : ExerciseTableActions {},
            )
        }
    }
}
