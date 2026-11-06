package com.everlog.android.exercise

import com.everlog.android.exercise.data.DeleteExerciseUseCase
import com.everlog.android.exercise.data.ExerciseRepository
import com.everlog.android.exercise.page.ExercisePageAnalytics
import com.everlog.android.workout.data.WorkoutRepository
import com.everlog.android.workout.data.exercisehistory.ExerciseStatsPreferences

import com.everlog.android.auth.data.UserSession
import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.store.CollectionStore
import com.everlog.android.common.navigation.Navigator
import com.everlog.android.exercise.data.ExerciseCopies
import com.everlog.android.exercise.data.ExerciseImageStorage
import com.everlog.android.exercise.data.LoadExerciseGuideUseCase
import com.everlog.android.exercise.data.LoadExerciseLibraryUseCase
import com.everlog.android.exercise.picker.ExercisePickerAnalytics
import com.everlog.android.settings.data.UserSettings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo

// What the exercise screens get from the app's graph, through appGraph<ExerciseGraph>()
@ContributesTo(AppScope::class)
interface ExerciseGraph {
    val userSettings: UserSettings

    val userSession: UserSession

    val exerciseImageStorage: ExerciseImageStorage

    val exerciseCopies: ExerciseCopies

    val loadExerciseLibrary: LoadExerciseLibraryUseCase

    val loadExerciseGuide: LoadExerciseGuideUseCase

    val exerciseAnalytics: ExerciseAnalytics

    val pickerAnalytics: ExercisePickerAnalytics

    val navigator: Navigator

    val exerciseConfig: ExerciseConfig

    // The user's workouts, for the exercise's statistics and history
    val workoutsStore: CollectionStore<ELWorkout>

    val workoutRepository: WorkoutRepository

    val exerciseRepository: ExerciseRepository

    val deleteExercise: DeleteExerciseUseCase

    val exercisePageAnalytics: ExercisePageAnalytics

    val exerciseStatsPreferences: ExerciseStatsPreferences
}
