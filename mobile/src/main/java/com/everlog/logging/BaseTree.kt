package com.everlog.logging

import android.util.Log
import timber.log.Timber

abstract class BaseTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        if (canLogPriority(priority)) {
            if (priority == Log.ERROR || throwable != null) {
                logError(priority, tag, message, throwable)
            } else {
                logMessage(priority, tag, message)
            }
        }
    }

    internal open fun canLogPriority(priority: Int): Boolean {
        return priority != Log.DEBUG
    }

    internal open fun logMessage(priority: Int, tag: String?, message: String) {
        // No-op
    }

    internal open fun logError(priority: Int, tag: String?, message: String?, throwable: Throwable?) {
        // No-op
    }
}