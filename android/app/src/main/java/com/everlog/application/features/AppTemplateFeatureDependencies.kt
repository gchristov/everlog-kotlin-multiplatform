package com.everlog.application.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.set.ELSetType
import com.everlog.android.common.sessionui.exercisetable.ExerciseTab
import com.everlog.android.exercise.data.RealExerciseRepository
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.exercise.details.ExerciseDetailsActivity
import com.everlog.android.template.data.RealRoutineRepository
import com.everlog.android.template.data.session.LoadTemplateUseCase
import com.everlog.android.template.data.session.RealLoadTemplateUseCase
import com.everlog.android.template.data.session.RealSaveTemplateUseCase
import com.everlog.android.template.data.session.SaveTemplateUseCase
import com.everlog.android.template.TemplateAnalytics
import com.everlog.android.template.TemplateFeatureDependencies
import com.everlog.android.template.editor.TemplateEditorAnalytics
import com.everlog.config.FeedbackSource
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.repositories.RealOngoingPlanRepository
import com.everlog.data.sync.AppDataSync
import com.everlog.managers.PlanManager
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity
import com.everlog.ui.navigator.ELNavigator
import com.everlog.ui.navigator.feedbackFormLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

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

    override fun openExerciseDetails(context: Context, exercise: ELExercise, tab: ExerciseTab) {
        val type = when (tab) {
            ExerciseTab.Info -> ExerciseDetailsActivity.Companion.Type.INFO
            ExerciseTab.History -> ExerciseDetailsActivity.Companion.Type.HISTORY
        }
        ELNavigator(context).openExerciseDetails(ExerciseDetailsActivity.Companion.Properties().exercise(exercise).type(type))
    }

    override fun openExercisePicker(activity: Activity) = ELNavigator(activity).openExercisePicker()

    override fun pickedExerciseGroups(requestCode: Int, resultCode: Int, data: Intent?): List<ELExerciseGroup>? =
        if (requestCode == ELActivityRequestCodes.REQUEST_PICK_EXERCISES && resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            data?.getSerializableExtra(ELConstants.EXTRA_EXERCISE_GROUPS) as? List<ELExerciseGroup>
        } else {
            null
        }

    override fun loadTemplate(): LoadTemplateUseCase =
        RealLoadTemplateUseCase(Dispatchers.Default, routines(), RealExerciseRepository(Dispatchers.IO, AppDataSync.instance))

    override fun saveTemplate(context: Context): SaveTemplateUseCase =
        RealSaveTemplateUseCase(Dispatchers.IO, routines(), RealOngoingPlanRepository(context.applicationContext))

    override fun ownExerciseUpdates(): Flow<List<LibraryExercise>> =
        RealExerciseRepository(Dispatchers.IO, AppDataSync.instance).ownExerciseUpdates()

    override fun templateEditorAnalytics(): TemplateEditorAnalytics = AppTemplateEditorAnalytics()

    private fun routines() = RealRoutineRepository(Dispatchers.IO, AppDataSync.instance) { ELDatastore.routineStore() }

    override fun removeFromOngoingPlan(activity: Activity, routine: ELRoutine) = PlanManager.manager.deleteRoutineForPlan(activity, routine)

    override fun setTypesFeedback(activity: Activity): (() -> Unit)? = activity.feedbackFormLauncher(FeedbackSource.SET_TYPES)
}
