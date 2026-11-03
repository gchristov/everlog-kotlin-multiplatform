package com.everlog.android.auth

import android.app.Activity
import android.content.Context

// The other screens the login screens open, through the app's Navigator
interface AuthNavigation {
    fun openHome(activity: Activity)

    fun openOnboarding(activity: Activity)

    fun openTerms(context: Context, title: String)

    fun openPrivacyPolicy(context: Context, title: String)
}
