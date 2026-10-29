package com.everlog.gradleplugins.android

import com.everlog.gradleplugins.android
import com.everlog.gradleplugins.catalogLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidBasePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // The application plugin applies this one after itself; any other module is a library
            if (!pluginManager.hasPlugin("com.android.application")) {
                pluginManager.apply("com.android.library")
            }
            pluginManager.apply("org.jetbrains.kotlin.android")
            pluginManager.apply("org.jetbrains.kotlin.plugin.parcelize")

            extensions.configure<KotlinAndroidProjectExtension> {
                // When changing the Java version make sure to also update the CI
                jvmToolchain(20)
            }
            with(android) {
                compileSdk = 36
                defaultConfig.minSdk = 24
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                compileOptions.isCoreLibraryDesugaringEnabled = true
            }
            dependencies {
                add("coreLibraryDesugaring", catalogLibrary("desugar-jdk-libs"))
            }
            configurations.configureEach {
                resolutionStrategy.dependencySubstitution {
                    substitute(module("org.hamcrest:hamcrest-core")).using(module("org.hamcrest:hamcrest:3.0"))
                    substitute(module("org.hamcrest:hamcrest-library")).using(module("org.hamcrest:hamcrest:3.0"))
                }
            }
        }
    }
}
