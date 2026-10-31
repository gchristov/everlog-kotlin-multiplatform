package com.everlog.android.auth.data

import com.everlog.android.auth.data.model.ELUser

// What signing in, registering, resetting a password or signing out reports back
open class AuthActionListener {
    open fun onLogout() {}
    open fun onSuccess(user: ELUser) {}
    // Login and registration. A new account has no data yet.
    open fun onSuccess(user: ELUser, newUser: Boolean) = onSuccess(user)
    open fun onResetPasswordSuccess() {}
    open fun onError(throwable: Throwable) {}
}
