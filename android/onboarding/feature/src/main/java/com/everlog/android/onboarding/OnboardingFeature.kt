package com.everlog.android.onboarding

import android.content.Context
import android.content.Intent
import com.everlog.android.onboarding.data.BuildStarterRoutinesUseCase
import com.everlog.android.onboarding.data.SaveStarterRoutinesUseCase

/**
 * What the onboarding needs from the app: services built on the app's data sync and managers, and the template
 * editor, which belongs to another feature. The app registers its implementation in [OnboardingFeature] at startup.
 * Shaped like constructor injection so a DI framework can take over the registration (android/README.md).
 */
interface OnboardingFeatureDependencies {
    fun buildStarterRoutines(): BuildStarterRoutinesUseCase

    fun saveStarterRoutines(): SaveStarterRoutinesUseCase

    fun onboardingSettings(context: Context): OnboardingSettings

    fun onboardingAnalytics(): OnboardingAnalytics

    // The template editor for a new template, which hands back RESULT_OK once it's saved
    fun newTemplateIntent(context: Context): Intent
}

object OnboardingFeature {
    lateinit var dependencies: OnboardingFeatureDependencies
}
