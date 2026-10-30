package com.everlog.gradleplugins.android

import com.android.build.api.dsl.LibraryExtension
import com.everlog.gradleplugins.android
import com.everlog.gradleplugins.catalogLibrary
import com.everlog.gradleplugins.libs
import com.everlog.gradleplugins.modules.enforceModuleRules
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
            enforceModuleRules()

            extensions.configure<KotlinAndroidProjectExtension> {
                // When changing the Java version make sure to also update the CI
                jvmToolchain(20)
            }
            with(android) {
                compileSdk = SDK
                defaultConfig.minSdk = 24
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                compileOptions.isCoreLibraryDesugaringEnabled = true
            }
            // A library's test APK and lint would otherwise target its min SDK, and a test APK targeting an old SDK
            // runs in compatibility mode on new devices
            extensions.findByType(LibraryExtension::class.java)?.apply {
                testOptions.targetSdk = SDK
                lint.targetSdk = SDK
            }
            dependencies {
                add("coreLibraryDesugaring", catalogLibrary("desugar-jdk-libs"))
                // ThreeTenABP only loads its time-zone data on Android, so JVM unit tests use the plain threetenbp,
                // which carries it. Instrumented tests mustn't get it: it duplicates ThreeTenABP's classes there.
                add("testImplementation", catalogLibrary("threetenbp"))
            }
            // JUnit 4 and Espresso still depend on Hamcrest 1.3's hamcrest-core and hamcrest-library. Since 2.1 Hamcrest
            // ships as the single hamcrest artifact, which the tests use directly, so without this both versions' classes
            // end up on the test classpath.
            val hamcrest = "org.hamcrest:hamcrest:${libs.findVersion("hamcrest").get().requiredVersion}"
            configurations.configureEach {
                resolutionStrategy.dependencySubstitution {
                    substitute(module("org.hamcrest:hamcrest-core")).using(module(hamcrest))
                    substitute(module("org.hamcrest:hamcrest-library")).using(module(hamcrest))
                }
            }
        }
    }
}

// The SDK the app and every module compile against and target
internal const val SDK = 36
