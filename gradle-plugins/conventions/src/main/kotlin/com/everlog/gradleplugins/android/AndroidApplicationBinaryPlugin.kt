package com.everlog.gradleplugins.android

import com.android.build.api.dsl.ApplicationExtension
import com.everlog.gradleplugins.applyCatalogPlugin
import com.everlog.gradleplugins.calculateVersionCode
import com.everlog.gradleplugins.calculateVersionName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationBinaryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            applyCatalogPlugin("everlog-android-base")
            extensions.configure<ApplicationExtension> {
                defaultConfig {
                    targetSdk = 36
                    versionCode = calculateVersionCode()
                    versionName = calculateVersionName()
                }
            }
        }
    }
}
