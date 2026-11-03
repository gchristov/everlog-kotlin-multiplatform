package com.everlog.android.common.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent

// Opens the app's screens. The app implements it; features inject it to open each other's screens and the app's.
interface Navigator {
    fun open(context: Context, destination: Destination)

    // Whether [destination] is there to open, e.g. a feedback form Remote Config has, so screens can leave its entry out
    fun canOpen(destination: Destination): Boolean

    // The intent that opens [destination], for an activity result launcher
    fun intent(context: Context, destination: ResultDestination<*>): Intent
}

// Opens [destination] for its result, which onActivityResult reads with its ScreenResult
fun Navigator.openForResult(activity: Activity, destination: ResultDestination<*>) =
    activity.startActivityForResult(intent(activity, destination), destination.screenResult.requestCode)
