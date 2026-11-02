package com.everlog.application

import com.everlog.android.auth.data.RealUserSession
import com.everlog.android.settings.data.RealUserSettings

// The one instance of each service the app hands to the features, until dependency injection creates them
object AppServices {
    @JvmField
    val userSettings = RealUserSettings()

    // One instance only: it keeps the user in memory once read
    @JvmField
    val userSession = RealUserSession()
}
