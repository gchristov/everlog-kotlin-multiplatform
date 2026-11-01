package com.everlog.gradleplugins

import org.gradle.api.GradleException
import org.gradle.api.Project

// See tools/versioning/README.md for how CI sets these properties
internal fun Project.calculateVersionName(): String {
    val baseVersion = readVersionFile("version.txt")
    val suffix = findProperty("ciVersionNameSuffix")?.toString()
    return when {
        suffix == "none" -> baseVersion
        suffix.isNullOrBlank() -> "$baseVersion-local"
        else -> "$baseVersion-$suffix"
    }
}

// Local builds take the version's base code (MmmP000), below every CI build of the version
internal fun Project.calculateVersionCode(): Int =
    findProperty("ciVersionCode")?.toString()?.toIntOrNull()
        ?: baseVersionCode(readVersionFile("version.txt"))

private fun baseVersionCode(version: String): Int {
    val (major, minor, patch) = Regex("""(\d)\.(\d{1,2})\.(\d)""").matchEntire(version)?.destructured
        ?: throw GradleException("tools/versioning/version.txt must be M.m.P with a one-digit major and patch, got $version")
    return major.toInt() * 1_000_000 + minor.toInt() * 10_000 + patch.toInt() * 1_000
}

private fun Project.readVersionFile(name: String): String {
    val versionFile = rootProject.file("tools/versioning/$name")
    if (!versionFile.exists()) {
        throw GradleException("tools/versioning/$name is missing. Required for versioning logic.")
    }
    return versionFile.readText().trim()
}
