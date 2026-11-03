package com.everlog.gradleplugins.modules

internal sealed interface Module {

    object App : Module

    // How data modules keep Firestore's cache in step with the server, on its own so only they link to it
    object DataSync : Module

    data class Common(val name: String) : Module {
        val isUi = name in setOf("compose", "compose-test", "design", "mvp", "session-ui")
    }

    data class Feature(val feature: String, val type: FeatureModuleType) : Module

    companion object {
        // :android:app, :android:datasync, :android:common:<name> or :android:<feature>:<type>
        fun of(path: String): Module? {
            val parts = path.removePrefix(":").split(":")
            if (parts.firstOrNull() != "android") return null
            return when {
                parts == listOf("android", "app") -> App
                parts == listOf("android", "datasync") -> DataSync
                parts.size == 3 && parts[1] == "common" -> Common(parts[2])
                parts.size == 3 -> FeatureModuleType.of(parts[2])?.let { Feature(parts[1], it) }
                else -> null
            }
        }
    }
}

// Code used only by other modules' tests
internal val Module.isTestSupport
    get() = when (this) {
        is Module.Common -> name == "test" || name.endsWith("-test")
        is Module.Feature -> type == FeatureModuleType.TEST_FIXTURES
        Module.App, Module.DataSync -> false
    }

internal enum class FeatureModuleType(val dirName: String) {
    DATA("data"),
    FEATURE("feature"),
    TEST_FIXTURES("test-fixtures");

    companion object {
        fun of(dirName: String) = values().firstOrNull { it.dirName == dirName }
    }
}
