package com.everlog.testutil

import timber.log.Timber

/**
 * Records errors logged through Timber, which the app sends to Crashlytics as non-fatals. Call
 * [uninstall] in `@After`.
 */
class RecordedErrors private constructor() : Timber.Tree() {
    val errors = mutableListOf<Throwable>()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (t != null) errors += t
    }

    companion object {
        fun install() = RecordedErrors().also { Timber.plant(it) }

        fun uninstall() = Timber.uprootAll()
    }
}
