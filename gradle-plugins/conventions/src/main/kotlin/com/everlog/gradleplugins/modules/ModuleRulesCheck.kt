package com.everlog.gradleplugins.modules

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

// Fails the build as soon as a module declares a dependency that breaks the module rules
internal fun Project.enforceModuleRules() {
    val from = Module.of(path) ?: return
    val fromPath = path
    configurations.configureEach {
        val configuration = name
        dependencies.withType(ProjectDependency::class.java).configureEach {
            // AGP makes a module's instrumented tests depend on the module itself
            if (path == fromPath) return@configureEach
            val to = Module.of(path) ?: return@configureEach
            moduleRuleBroken(from, to, forTests = isTestConfiguration(configuration))?.let { reason ->
                throw GradleException("$fromPath can't depend on $path in $configuration: $reason. See android/README.md.")
            }
        }
    }
}

private fun isTestConfiguration(name: String) =
    name.startsWith("test") || name.startsWith("androidTest") || "UnitTest" in name || "AndroidTest" in name
