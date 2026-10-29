package com.everlog.gradleplugins

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.applyCatalogPlugin(alias: String) {
    pluginManager.apply(libs.findPlugin(alias).get().get().pluginId)
}

internal fun Project.catalogLibrary(alias: String) = libs.findLibrary(alias).get()

// The DSL shared by app and library modules
internal val Project.android: CommonExtension<*, *, *, *, *, *>
    get() = extensions.getByType(CommonExtension::class.java)
