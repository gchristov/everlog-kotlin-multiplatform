package com.everlog.application.features

import android.app.Activity
import android.content.Intent
import com.everlog.android.auth.AuthService
import com.everlog.android.auth.data.AuthActionListener
import com.everlog.managers.auth.AuthManager
import com.everlog.ui.navigator.ELNavigator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppAuthService : AuthService {
    override fun login(email: String, password: String, listener: AuthActionListener) = AuthManager.login(email, password, listener)

    override fun loginAnonymously(listener: AuthActionListener) = AuthManager.loginAnonymously(listener)

    override fun loginWithGoogle(activity: Activity, listener: AuthActionListener) = AuthManager.loginWithGoogle(ELNavigator(activity), listener)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        AuthManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun register(fullName: String, email: String, password: String, listener: AuthActionListener) =
        AuthManager.register(fullName, email, password, listener)

    override fun requestPasswordReset(email: String, listener: AuthActionListener) = AuthManager.requestPasswordReset(email, listener)
}
