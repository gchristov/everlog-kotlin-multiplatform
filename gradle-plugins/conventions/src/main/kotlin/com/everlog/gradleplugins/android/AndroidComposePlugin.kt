package com.everlog.gradleplugins.android

import com.everlog.gradleplugins.CommonModules
import com.everlog.gradleplugins.android
import com.everlog.gradleplugins.catalogLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            android.buildFeatures.compose = true
            // Feature tests run screens on the JVM with Robolectric (FeatureTest in common:compose-test), which needs
            // the module's resources
            android.testOptions.unitTests.isIncludeAndroidResources = true
            if (path != CommonModules.COMPOSE_TEST) {
                dependencies {
                    // Unit tests only: on a device, AndroidJUnit4 hands tests to Robolectric whenever it's there
                    add("testImplementation", catalogLibrary("robolectric"))
                    if (path != CommonModules.COMPOSE) add("testImplementation", project(CommonModules.COMPOSE_TEST))
                }
            }
        }
    }
}
