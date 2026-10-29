package com.everlog.gradleplugins.android

import com.everlog.gradleplugins.CommonModules
import com.everlog.gradleplugins.applyCatalogPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidModulePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            applyCatalogPlugin("everlog-android-base")
            dependencies {
                add("testImplementation", project(CommonModules.TEST))
            }
        }
    }
}
