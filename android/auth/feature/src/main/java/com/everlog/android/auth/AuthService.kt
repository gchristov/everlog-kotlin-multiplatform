package com.everlog.android.auth

import android.app.Activity
import android.content.Intent
import com.everlog.android.auth.data.AuthActionListener

// Signing in and up, which the app does with Firebase Auth
interface AuthService {
    fun login(email: String, password: String, listener: AuthActionListener)

    fun loginAnonymously(listener: AuthActionListener)

    fun loginWithGoogle(activity: Activity, listener: AuthActionListener)

    // Google sign-in's answer, from onActivityResult
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)

    fun register(fullName: String, email: String, password: String, newsletter: Boolean, listener: AuthActionListener)

    fun requestPasswordReset(email: String, listener: AuthActionListener)
}
