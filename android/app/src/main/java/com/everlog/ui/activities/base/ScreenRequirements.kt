package com.everlog.ui.activities.base

import android.app.Activity
import com.everlog.android.common.compose.hasRequiredExtras
import com.everlog.managers.auth.LocalUserManager
import timber.log.Timber

fun Activity.canOpen(requiredExtras: List<String>, requiresUser: Boolean): Boolean =
    hasRequiredExtras(requiredExtras) && (!requiresUser || hasUser())

// As with the required extras, a screen opened without a user records a non-fatal and closes
fun Activity.hasUser(): Boolean {
    if (!LocalUserManager.hasUser()) {
        Timber.tag(javaClass.simpleName).e("Closing screen opened without a logged in user")
        return false
    }
    return true
}
