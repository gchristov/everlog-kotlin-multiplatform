package com.everlog.android.onboarding

import android.content.Context
import android.content.Intent

// The other screens the onboarding opens, through the app's Navigator
interface OnboardingNavigation {
    // The template editor for a new template, which hands back RESULT_OK once it's saved
    fun newTemplateIntent(context: Context): Intent
}
