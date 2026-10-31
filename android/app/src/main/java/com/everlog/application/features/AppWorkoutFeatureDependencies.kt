package com.everlog.application.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.common.sessionui.exercisetable.ExerciseTab
import com.everlog.android.exercise.data.RealExerciseRepository
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.workout.data.OngoingWorkoutRepository
import com.everlog.android.workout.data.RealWorkoutRepository
import com.everlog.android.workout.data.prefill.LoadPrefillSourcesUseCase
import com.everlog.android.workout.data.prefill.RealLoadPrefillSourcesUseCase
import com.everlog.android.workout.details.WorkoutDetailsAnalytics
import com.everlog.android.workout.details.WorkoutTextField
import com.everlog.android.workout.WorkoutFeatureDependencies
import com.everlog.android.workout.log.WorkoutLogAlerts
import com.everlog.android.workout.log.WorkoutLogAnalytics
import com.everlog.android.workout.log.WorkoutLogNotification
import com.everlog.android.workout.notification.WorkoutServiceAnalytics
import com.everlog.android.workout.setsuggestions.SetSuggestionsAnalytics
import com.everlog.android.workout.setsuggestions.SetSuggestionsSettings
import com.everlog.config.AppConfig
import com.everlog.config.FeedbackSource
import com.everlog.constants.ELActivityRequestCodes
import com.everlog.constants.ELConstants
import com.everlog.data.controllers.statistics.UserStatsController
import com.everlog.data.repositories.RealOngoingPlanRepository
import com.everlog.data.repositories.RealOngoingWorkoutRepository
import com.everlog.data.sync.AppDataSync
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.managers.apprate.RatePromptTrigger
import com.everlog.ui.activities.home.exercisegroup.DefaultCreateExerciseGroupsActivity
import com.everlog.ui.dialog.AppDialogs
import com.everlog.ui.navigator.ELNavigator
import com.everlog.ui.navigator.feedbackFormLauncher
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import rx.Observable

class AppWorkoutFeatureDependencies : WorkoutFeatureDependencies {
    private val templates = AppTemplateFeatureDependencies()

    override fun ongoingWorkoutRepository(): OngoingWorkoutRepository = RealOngoingWorkoutRepository()

    override fun loadPrefillSources(): LoadPrefillSourcesUseCase = RealLoadPrefillSourcesUseCase(
        Dispatchers.Default,
        RealWorkoutRepository(Dispatchers.IO, AppDataSync.instance, AppConfig.configuration.workoutSyncMonths),
    )

    override fun ownExerciseUpdates(): Flow<List<LibraryExercise>> =
        RealExerciseRepository(Dispatchers.IO, AppDataSync.instance).ownExerciseUpdates()

    override fun workoutLogAnalytics(): WorkoutLogAnalytics = AppWorkoutLogAnalytics()

    override fun workoutNotification(context: Context): WorkoutLogNotification = AppWorkoutLogNotification(ELNavigator(context))

    override fun timerAlerts(context: Context): WorkoutLogAlerts = AppWorkoutLogAlerts(context)

    override fun workoutServiceAnalytics(): WorkoutServiceAnalytics = object : WorkoutServiceAnalytics {
        private val analytics = AnalyticsManager.manager

        override fun weightModified() = analytics.workoutServiceWeightModified()

        override fun repsModified() = analytics.workoutServiceRepsModified()

        override fun timerStartedExercise() = analytics.workoutServiceTimerStartedExercise()

        override fun timerStoppedExercise() = analytics.workoutServiceTimerStoppedExercise()

        override fun timerStoppedRest() = analytics.workoutServiceTimerStoppedRest()

        override fun nextExercise() = analytics.workoutServiceNextExercise()
    }

    override fun openExercisePicker(activity: Activity) = templates.openExercisePicker(activity)

    override fun pickedExerciseGroups(requestCode: Int, resultCode: Int, data: Intent?): List<ELExerciseGroup>? =
        templates.pickedExerciseGroups(requestCode, resultCode, data)

    override fun openExerciseDetails(context: Context, exercise: ELExercise, tab: ExerciseTab) =
        templates.openExerciseDetails(context, exercise, tab)

    override fun openSetSuggestions(activity: Activity) = ELNavigator(activity).openMuscleGoal()

    override fun openWorkoutDetails(activity: Activity, workout: ELWorkout, fromPlan: Boolean) =
        ELNavigator(activity).openWorkoutDetails(workout, true, fromPlan)

    override fun promptForAppSettings(context: Context, rationaleResId: Int) =
        ELNavigator(context).promptForAppSettings(rationaleResId)

    override fun workoutFeedback(activity: Activity): (() -> Unit)? = activity.feedbackFormLauncher(FeedbackSource.WORKOUT_MENU)

    override fun setSuggestionsFeedback(activity: Activity): (() -> Unit)? =
        activity.feedbackFormLauncher(FeedbackSource.SET_SUGGESTIONS)

    override fun setSuggestionsSettings(context: Context): SetSuggestionsSettings = AppSetSuggestionsSettings(context)

    override fun setSuggestionsAnalytics(): SetSuggestionsAnalytics = AppSetSuggestionsAnalytics()

    override fun workoutDetailsAnalytics(): WorkoutDetailsAnalytics = object : WorkoutDetailsAnalytics {
        private val analytics = AnalyticsManager.manager

        override fun shared() = analytics.workoutDetailsShared()

        override fun deleted() = analytics.workoutDetailsDeleted()

        override fun nameModified() = analytics.workoutDetailsNameModified()

        override fun dateModified() = analytics.workoutDetailsDateModified()

        override fun notesModified() = analytics.workoutDetailsNotesModified()

        override fun exercisesModified() = analytics.workoutDetailsExercisesModified()

        override fun savedAsTemplate() {
            analytics.workoutDetailsSavedAsRoutine()
            analytics.routineCreated()
        }
    }

    override fun shareWorkout(activity: Activity, workout: ELWorkout) = ELNavigator(activity).shareWorkout(workout)

    override fun requestRatePrompt(activity: Activity, workout: ELWorkout, delayMillis: Long) =
        ELNavigator(activity).requestRatePrompt(RatePromptTrigger.WorkoutCompleted(workout), delayMillis)

    override fun replaceRoutineInOngoingPlan(context: Context, routine: ELRoutine) =
        RealOngoingPlanRepository(context.applicationContext).replaceRoutine(routine)

    override fun workoutsThisWeek(workouts: List<ELWorkout>, onCount: (Int) -> Unit, onError: (Throwable) -> Unit) =
        UserStatsController().loadStats(RangeType.WEEK, workouts, object : UserStatsController.OnCompleteListener {
            override fun onComplete(result: UserStatsController.StatsResult) = onCount(result.workoutsCompleted)

            override fun onError(throwable: Throwable) = onError(throwable)
        })

    override fun showTextPrompt(context: Context, value: String?, field: WorkoutTextField): Observable<String> =
        AppDialogs.showInputStringDialog(context, value, when (field) {
            WorkoutTextField.Name -> AppDialogs.StringDialogType.WORKOUT_NAME
            WorkoutTextField.Note -> AppDialogs.StringDialogType.WORKOUT_NOTE
        })

    override fun showDateTimePrompt(context: Context, date: Date): Observable<Date> = AppDialogs.showDateTimeDialog(context, date)

    override fun openExerciseEditor(activity: Activity, groups: List<ELExerciseGroup>) =
        ELNavigator(activity).openEditExerciseGroups(DefaultCreateExerciseGroupsActivity.Companion.Properties().groups(groups))

    override fun editedExerciseGroups(requestCode: Int, resultCode: Int, data: Intent?): List<ELExerciseGroup>? =
        if (requestCode == ELActivityRequestCodes.REQUEST_EDIT_EXERCISE_GROUPS && resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION", "UNCHECKED_CAST")
            data?.getSerializableExtra(ELConstants.EXTRA_EXERCISE_GROUPS) as? List<ELExerciseGroup>
        } else {
            null
        }
}
