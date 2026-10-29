package com.everlog.logging

import android.util.Log
import com.hypertrack.hyperlog.HyperLog

class HyperlogTree : BaseTree() {

    override fun logMessage(priority: Int, tag: String?, message: String) {
        when (priority) {
            Log.VERBOSE -> HyperLog.v(tag, message)
            Log.INFO -> HyperLog.i(tag, message)
            Log.WARN -> HyperLog.w(tag, message)
            Log.ASSERT -> HyperLog.a(message)
        }
    }

    override fun logError(priority: Int, tag: String?, message: String?, throwable: Throwable?) {
        if (throwable != null) {
            HyperLog.exception(tag, message, throwable)
        } else {
            HyperLog.e(tag, message)
        }
    }
}