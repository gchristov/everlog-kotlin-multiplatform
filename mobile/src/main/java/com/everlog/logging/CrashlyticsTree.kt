package com.everlog.logging

import com.everlog.logging.LogFormatter.Companion.format
import com.google.firebase.crashlytics.FirebaseCrashlytics

class CrashlyticsTree : BaseTree() {

    override fun logMessage(priority: Int, tag: String?, message: String) {
        FirebaseCrashlytics.getInstance().log(format(priority, tag, message))
    }

    override fun logError(priority: Int, tag: String?, message: String?, throwable: Throwable?) {
        if (throwable != null) {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        } else {
            FirebaseCrashlytics.getInstance().recordException(StackTraceRecorder(format(priority, tag, message)))
        }
    }
}