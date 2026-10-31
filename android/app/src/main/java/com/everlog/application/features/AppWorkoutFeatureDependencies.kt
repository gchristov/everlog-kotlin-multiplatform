package com.everlog.application.features

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.sessionui.exercisetable.ExerciseTab
import com.everlog.android.exercise.data.RealExerciseRepository
import com.everlog.android.exercise.data.model.library.LibraryExercise
import com.everlog.android.workout.data.OngoingWorkoutRepository
import com.everlog.android.workout.data.RealWorkoutRepository
import com.everlog.android.workout.data.prefill.LoadPrefillSourcesUseCase
import com.everlog.android.workout.data.prefill.RealLoadPrefillSourcesUseCase
import com.everlog.android.workout.feature.WorkoutFeatureDependencies
import com.everlog.android.workout.feature.log.WorkoutLogAlerts
import com.everlog.android.workout.feature.log.WorkoutLogAnalytics
import com.everlog.android.workout.feature.log.WorkoutLogNotification
import com.everlog.android.workout.feature.notification.WorkoutServiceAnalytics
import com.everlog.android.workout.feature.setsuggestions.SetSuggestionsAnalytics
import com.everlog.android.workout.feature.setsuggestions.SetSuggestionsSettings
import com.everlog.config.AppConfig
import com.everlog.config.FeedbackSource
import com.everlog.data.repositories.RealOngoingWorkoutRepository
import com.everlog.data.sync.AppDataSync
import com.everlog.managers.analytics.AnalyticsManager
import com.everlog.ui.navigator.ELNavigator
import com.everlog.ui.navigator.feedbackFormLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

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
}
