package com.everlog.android.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.everlog.android.auth.data.AuthActionListener

/**
 * What the auth feature needs from the app: signing in through the app's AuthManager, and the screens of other
 * features, which open through the app's Navigator. The app registers its implementation in [AuthFeature] at
 * startup. Shaped like constructor injection so a DI framework can take over the registration (android/README.md).
 */
interface AuthFeatureDependencies {
    fun login(email: String, password: String, listener: AuthActionListener)

    fun loginAnonymously(listener: AuthActionListener)

    fun loginWithGoogle(activity: Activity, listener: AuthActionListener)

    // Google sign-in's answer for loginWithGoogle, from onActivityResult
    fun authActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    fun register(fullName: String, email: String, password: String, newsletter: Boolean, listener: AuthActionListener)

    fun requestPasswordReset(email: String, listener: AuthActionListener)

    // Whether a new account still has onboarding to finish, kept in the app's settings
    fun onboardingPending(): Boolean

    fun setOnboardingPending(pending: Boolean)

    fun openHome(activity: Activity)

    // Onboarding, which hands back its result with AuthRequestCodes.ONBOARDING
    fun openOnboarding(activity: Activity)

    fun openTerms(context: Context, title: String)

    fun openPrivacyPolicy(context: Context, title: String)

    // Nobody to remind while signed out
    fun cancelAppUseReminder(context: Context)
}

object AuthFeature {
    lateinit var dependencies: AuthFeatureDependencies
}

object AuthRequestCodes {
    const val ONBOARDING = 9
}

// The names analytics already has for the auth screens
object AuthScreenNames {
    const val LOGIN = "screen_login"
    const val RESET_PASSWORD = "screen_reset_password"
}
