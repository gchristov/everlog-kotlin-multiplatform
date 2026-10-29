package com.everlog.gradleplugins.android

import com.everlog.gradleplugins.CommonModules
import com.everlog.gradleplugins.applyCatalogPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyCatalogPlugin("everlog-android-module")
            applyCatalogPlugin("everlog-android-compose")
            dependencies {
                add("implementation", project(CommonModules.COMPOSE))
                add("implementation", project(CommonModules.DESIGN))
                add("implementation", project(CommonModules.MVVM))
                add("androidTestImplementation", project(CommonModules.COMPOSE_TEST))
            }
        }
    }
}
