package com.everlog.android.common.data

/**
 * What the data layer needs from the app. The app implements it and registers it in `ELApplication.onCreate`,
 * as with the features' dependencies (see `android/README.md`).
 */
interface CommonDataDependencies {
    // Test Lab and Play pre-launch runs don't write to Firestore
    fun isTestLabRun(): Boolean
}

object CommonData {
    lateinit var dependencies: CommonDataDependencies
}
