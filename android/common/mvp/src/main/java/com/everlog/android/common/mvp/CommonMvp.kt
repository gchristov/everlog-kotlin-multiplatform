package com.everlog.android.common.mvp

/**
 * What the MVP bases need from the app. The app implements it and registers it in `ELApplication.onCreate`,
 * as with the features' dependencies (see `android/README.md`).
 */
interface CommonMvpDependencies {
    fun screenViewed(name: String)
    fun hasUser(): Boolean
    // Test Lab and Play pre-launch runs skip screen tips, which block Robo scripts
    fun isTestLabRun(): Boolean
}

object CommonMvp {
    lateinit var dependencies: CommonMvpDependencies
}
