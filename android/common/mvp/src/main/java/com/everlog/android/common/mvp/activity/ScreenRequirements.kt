package com.everlog.android.common.mvp.activity

import android.app.Activity
import com.everlog.android.common.kotlin.hasRequiredExtras
import com.everlog.android.common.mvp.CommonMvp
import timber.log.Timber

fun Activity.canOpen(requiredExtras: List<String>, requiresUser: Boolean): Boolean =
    hasRequiredExtras(requiredExtras) && (!requiresUser || hasUser())

// As with the required extras, a screen opened without a user records a non-fatal and closes
fun Activity.hasUser(): Boolean {
    if (!CommonMvp.dependencies.hasUser()) {
        Timber.tag(javaClass.simpleName).e("Closing screen opened without a logged in user")
        return false
    }
    return true
}
