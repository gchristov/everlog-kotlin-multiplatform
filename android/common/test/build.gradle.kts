plugins {
    alias(libs.plugins.everlog.android.base)
}

android {
    namespace = "com.everlog.android.common.test"
}

// Every module's tests get this through android-module-plugin, so it applies the base plugin rather than depend on
// itself. Test support lives in main, with its dependencies as api, because a module's test sources can't be shared.

dependencies {
    api(libs.junit)
    api(libs.kotlin.test.junit)
    api(libs.truth)
    api(libs.kotlinx.coroutines.android)
    // The preferences fakes install themselves in PreferencesManager
    api(projects.android.common.persistence)
}
