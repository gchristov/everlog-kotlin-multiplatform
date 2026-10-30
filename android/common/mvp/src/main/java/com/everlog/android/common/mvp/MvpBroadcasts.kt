package com.everlog.android.common.mvp

// Local broadcasts that every presenter of a kind listens to
object MvpBroadcasts {
    const val PREFERENCES_CHANGED = "com.everlog.BROADCAST_PREFERENCES_CHANGED"
    const val PRO_CHANGED = "com.everlog.BROADCAST_PRO_CHANGED"
    const val REMOTE_CONFIG_REFRESHED = "com.everlog.BROADCAST_REMOTE_CONFIG_REFRESHED"
    // Screens that shouldn't stay open behind a workout close when one starts
    const val WORKOUT_STARTED = "com.everlog.BROADCAST_WORKOUT_STARTED"
}
