package com.everlog.gradleplugins

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.util.Properties

// One file for every module, written by tools/secrets/secrets.sh on CI
fun Project.envSecret(key: String): String {
    val properties = Properties()
    rootProject.file("android/app/secrets.properties").inputStream().use(properties::load)
    val property = properties.getProperty(key)
    if (property.isNullOrBlank()) {
        throw GradleException("Required property is missing: property=$key")
    }
    return property
}
