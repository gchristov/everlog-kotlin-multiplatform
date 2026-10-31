package com.everlog.application.features

import android.app.Activity
import android.content.Context
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.exercise.feature.details.ExerciseDetailsActivity
import com.everlog.android.template.feature.TemplateAnalytics
import com.everlog.android.template.feature.TemplateFeatureDependencies
import com.everlog.config.FeedbackSource
import com.everlog.managers.PlanManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity
import com.everlog.ui.navigator.ELNavigator
import com.everlog.ui.navigator.feedbackFormLauncher

class AppTemplateFeatureDependencies : TemplateFeatureDependencies {

    override fun templateAnalytics(): TemplateAnalytics = object : TemplateAnalytics {
        override fun routineDeleted() = AnalyticsManager.manager.routineDeleted()

        override fun setTypeSelected(type: ELSetType?) = AnalyticsManager.manager.setTypeSelected(type)
    }

    override fun openTemplateEditor(activity: Activity, routine: ELRoutine?) = ELNavigator(activity).openEditRoutine(
        CreateRoutineActivity.Companion.Properties().routine(routine).showDetailsOnSuccess(false),
    )

    override fun startWorkout(activity: Activity, routine: ELRoutine, fromPlan: Boolean) =
        ELNavigator(activity).startWorkout(routine, true, fromPlan)

    override fun openExerciseDetails(context: Context, exercise: ELExercise) =
        ELNavigator(context).openExerciseDetails(ExerciseDetailsActivity.Companion.Properties().exercise(exercise))

    override fun removeFromOngoingPlan(activity: Activity, routine: ELRoutine) = PlanManager.manager.deleteRoutineForPlan(activity, routine)

    override fun setTypesFeedback(activity: Activity): (() -> Unit)? = activity.feedbackFormLauncher(FeedbackSource.SET_TYPES)
}
