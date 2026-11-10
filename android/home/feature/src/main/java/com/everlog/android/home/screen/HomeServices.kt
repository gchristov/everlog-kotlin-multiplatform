package com.everlog.android.home.screen

import com.everlog.android.common.data.model.workout.ELWorkout

// What the Compose Home needs from the app, which the app binds
interface HomeServices {
    fun ongoingWorkout(): ELWorkout?

    // Ends the ongoing workout without saving it, and its notification
    fun discardOngoingWorkout()

    // Once, when Home opens: who's signed in for analytics and crash reports, the push token, pending exercise
    // image uploads, and the launch count the rating prompt goes by
    fun onStarted()

    // Each time Home comes to the front: no app use reminder while it's open, and fresh Remote Config
    fun onResumed()

    // Home going to the back: the app use reminder for if the user doesn't come back
    fun onPaused()

    // Home closing: the old data stores' listeners go
    fun onDestroyed()
}
