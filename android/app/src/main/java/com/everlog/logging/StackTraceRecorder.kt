package com.everlog.logging

import timber.log.Timber
import java.util.*

class StackTraceRecorder constructor(detailMessage: String?) : Throwable(detailMessage) {

    fun fillInStackTrace(): Throwable? {
        super.fillInStackTrace()
        val original = stackTrace
        val iterator: Iterator<StackTraceElement> = Arrays.asList(*original).iterator()
        val filtered: MutableList<StackTraceElement> = ArrayList()

        // heading to top of Timber stack trace
        while (iterator.hasNext()) {
            val stackTraceElement = iterator.next()
            if (isTimber(stackTraceElement)) {
                break
            }
        }

        // copy all
        var isReachedApp = false
        while (iterator.hasNext()) {
            val stackTraceElement = iterator.next()
            // skip Timber
            if (!isReachedApp && isTimber(stackTraceElement)) {
                continue
            }
            isReachedApp = true
            filtered.add(stackTraceElement)
        }
        stackTrace = filtered.toTypedArray()
        return this
    }

    private fun isTimber(stackTraceElement: StackTraceElement): Boolean {
        return stackTraceElement.className == Timber::class.java.name
    }
}