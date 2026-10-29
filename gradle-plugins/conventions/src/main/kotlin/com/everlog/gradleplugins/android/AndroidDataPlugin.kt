package com.everlog.gradleplugins.android

import com.everlog.gradleplugins.CommonModules
import com.everlog.gradleplugins.applyCatalogPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidDataPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyCatalogPlugin("everlog-android-module")
            // A data module's API returns these modules' types (models, Load), so the features using it see them too
            dependencies {
                add("api", project(CommonModules.KOTLIN))
                add("api", project(CommonModules.FIREBASE))
                add("api", project(CommonModules.DATASYNC))
                add("api", project(CommonModules.DATA))
            }
        }
    }
}
