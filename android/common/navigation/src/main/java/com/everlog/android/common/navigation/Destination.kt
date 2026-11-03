package com.everlog.android.common.navigation

import android.app.Activity
import android.content.Intent

// A screen another feature or the app opens. A screen opened only from inside its own feature isn't one.
sealed interface Destination

// A screen that hands something back, read with its [screenResult]
sealed interface ResultDestination<R : Any> : Destination {
    val screenResult: ScreenResult<R>
}

// Reads what a screen handed back, in onActivityResult. Each result destination has one without needing an
// instance: its companion object, e.g. SetTypePicker.result(requestCode, resultCode, data).
interface ScreenResult<R : Any> {
    val requestCode: Int

    fun read(data: Intent?): R?

    // What the screen handed back, or null when it closed without one or the result is for another request
    fun result(requestCode: Int, resultCode: Int, data: Intent?): R? =
        if (requestCode == this.requestCode && resultCode == Activity.RESULT_OK) read(data) else null
}

// What screens hand back in their result intents
object NavigationExtras {
    const val EXERCISE_GROUPS = "EXTRA_EXERCISE_GROUPS"
    const val SET_TYPE = "EXTRA_SET_TYPE"
    const val ROUTINE = "EXTRA_ROUTINE"
}

@Suppress("DEPRECATION", "UNCHECKED_CAST")
internal fun <T> Intent?.serializable(key: String): T? = this?.getSerializableExtra(key) as? T
