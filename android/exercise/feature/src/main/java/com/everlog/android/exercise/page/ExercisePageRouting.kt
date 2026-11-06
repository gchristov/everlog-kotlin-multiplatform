package com.everlog.android.exercise.page

import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.kotlin.di.appGraph
import com.everlog.android.exercise.ExerciseGraph
import com.everlog.android.exercise.details.ExerciseDetailsActivity

/**
 * An exercise's page for every entry point: the Compose one while Remote Config's exercise_details_v2 is on, the old
 * one otherwise.
 */
fun exercisePageIntent(context: Context, exercise: ELExercise, tab: ExercisePageTab): Intent =
    if (context.appGraph<ExerciseGraph>().exerciseConfig.exerciseDetailsV2()) {
        ExercisePageActivity.launchIntent(context, exercise, tab)
    } else {
        ExerciseDetailsActivity.launchIntent(
            context,
            ExerciseDetailsActivity.Companion.Properties().exercise(exercise).type(
                when (tab) {
                    ExercisePageTab.Info -> ExerciseDetailsActivity.Companion.Type.INFO
                    ExercisePageTab.Statistics -> ExerciseDetailsActivity.Companion.Type.STATISTICS
                    ExercisePageTab.History -> ExerciseDetailsActivity.Companion.Type.HISTORY
                },
            ),
        )
    }
