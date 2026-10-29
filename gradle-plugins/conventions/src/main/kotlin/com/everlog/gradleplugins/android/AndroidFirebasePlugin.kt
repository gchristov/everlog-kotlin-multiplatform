package com.everlog.gradleplugins.android

import org.gradle.api.Plugin
import org.gradle.api.Project

// Only for the app: google-services needs an application module and its google-services.json
class AndroidFirebasePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target.pluginManager) {
            apply("com.google.gms.google-services")
            apply("com.google.firebase.crashlytics")
            apply("com.google.firebase.firebase-perf")
        }
    }
}
