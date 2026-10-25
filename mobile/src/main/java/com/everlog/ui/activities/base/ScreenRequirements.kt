package com.everlog.ui.activities.base

import android.app.Activity
import com.everlog.managers.auth.LocalUserManager
import timber.log.Timber

// Play pre-launch robots open screens directly, without what the app would pass them. Errors are
// recorded as non-fatals, so a bug that opens a screen this way still shows up.
fun Activity.canOpen(requiredExtras: List<String>, requiresUser: Boolean): Boolean {
    val missingExtras = requiredExtras.filter { intent?.extras?.get(it) == null }
    if (missingExtras.isNotEmpty()) {
        Timber.tag(javaClass.simpleName).e("Closing screen opened without required extras: %s", missingExtras)
        return false
    }
    if (requiresUser && !LocalUserManager.hasUser()) {
        Timber.tag(javaClass.simpleName).e("Closing screen opened without a logged in user")
        return false
    }
    return true
}
