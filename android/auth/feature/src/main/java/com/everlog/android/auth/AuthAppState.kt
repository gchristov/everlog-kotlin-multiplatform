package com.everlog.android.auth

import android.content.Context

// What signing in changes in the rest of the app
interface AuthAppState {
    // A new account that hasn't been through onboarding yet
    fun onboardingPending(): Boolean

    fun setOnboardingPending(pending: Boolean)

    fun cancelAppUseReminder(context: Context)
}
