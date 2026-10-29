package com.everlog.gradleplugins.android

import com.everlog.gradleplugins.android
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            android.buildFeatures.compose = true
        }
    }
}
