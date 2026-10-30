package com.everlog.android.common.kotlin

import android.app.Activity
import timber.log.Timber

// Play pre-launch robots open screens directly, without what the app would pass them. Errors are
// recorded as non-fatals, so a bug that opens a screen this way still shows up.
fun Activity.hasRequiredExtras(requiredExtras: List<String>): Boolean {
    val missingExtras = requiredExtras.filter { intent?.extras?.get(it) == null }
    if (missingExtras.isNotEmpty()) {
        Timber.tag(javaClass.simpleName).e("Closing screen opened without required extras: %s", missingExtras)
        return false
    }
    return true
}
