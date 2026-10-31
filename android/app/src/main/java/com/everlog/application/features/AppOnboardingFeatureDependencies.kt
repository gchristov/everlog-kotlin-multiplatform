package com.everlog.application.features

import android.content.Context
import android.content.Intent
import com.everlog.android.exercise.data.RealExerciseRepository
import com.everlog.android.onboarding.OnboardingAnalytics
import com.everlog.android.onboarding.OnboardingFeatureDependencies
import com.everlog.android.onboarding.OnboardingSettings
import com.everlog.android.onboarding.data.BuildStarterRoutinesUseCase
import com.everlog.android.onboarding.data.RealBuildStarterRoutinesUseCase
import com.everlog.android.onboarding.data.RealSaveStarterRoutinesUseCase
import com.everlog.android.onboarding.data.SaveStarterRoutinesUseCase
import com.everlog.android.template.data.RealRoutineRepository
import com.everlog.data.datastores.ELDatastore
import com.everlog.data.sync.AppDataSync
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity
import com.everlog.ui.activities.home.routine.create.templateEditorIntent
import kotlinx.coroutines.Dispatchers

class AppOnboardingFeatureDependencies : OnboardingFeatureDependencies {

    override fun buildStarterRoutines(): BuildStarterRoutinesUseCase = RealBuildStarterRoutinesUseCase(
        dispatcher = Dispatchers.Default,
        exerciseRepository = RealExerciseRepository(dispatcher = Dispatchers.IO, dataSync = AppDataSync.instance),
    )

    override fun saveStarterRoutines(): SaveStarterRoutinesUseCase = RealSaveStarterRoutinesUseCase(
        dispatcher = Dispatchers.Default,
        routineRepository = RealRoutineRepository(dispatcher = Dispatchers.IO, dataSync = AppDataSync.instance, testLabRoutines = { ELDatastore.routineStore() }),
    )

    override fun onboardingSettings(context: Context): OnboardingSettings = AppOnboardingSettings(context)

    override fun onboardingAnalytics(): OnboardingAnalytics = AppOnboardingAnalytics()

    // Not the template's details after saving: the onboarding decides what comes next
    override fun newTemplateIntent(context: Context): Intent =
        templateEditorIntent(context, CreateRoutineActivity.Companion.Properties().showDetailsOnSuccess(false))
}
