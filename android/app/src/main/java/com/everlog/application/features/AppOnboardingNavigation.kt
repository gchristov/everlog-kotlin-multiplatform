package com.everlog.application.features

import android.content.Context
import android.content.Intent
import com.everlog.android.onboarding.OnboardingNavigation
import com.everlog.ui.activities.home.routine.create.CreateRoutineActivity
import com.everlog.ui.activities.home.routine.create.templateEditorIntent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppOnboardingNavigation : OnboardingNavigation {
    // Not the template's details after saving: the onboarding decides what comes next
    override fun newTemplateIntent(context: Context): Intent =
        templateEditorIntent(context, CreateRoutineActivity.Companion.Properties().showDetailsOnSuccess(false))
}
