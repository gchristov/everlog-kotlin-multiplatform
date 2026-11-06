package com.everlog.ui.navigator

import android.content.Context
import android.content.Intent
import com.everlog.android.exercise.details.ExerciseDetailsActivity
import com.everlog.android.exercise.page.ExercisePageTab
import com.everlog.android.exercise.page.exercisePageIntent

// The app's old way of opening an exercise, through the feature's routing
fun exerciseDetailsIntent(context: Context, properties: ExerciseDetailsActivity.Companion.Properties): Intent =
    exercisePageIntent(
        context,
        properties.exercise,
        when (properties.type) {
            ExerciseDetailsActivity.Companion.Type.INFO -> ExercisePageTab.Info
            ExerciseDetailsActivity.Companion.Type.STATISTICS -> ExercisePageTab.Statistics
            ExerciseDetailsActivity.Companion.Type.HISTORY -> ExercisePageTab.History
        },
    )
