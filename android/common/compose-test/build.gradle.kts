plugins {
    alias(libs.plugins.everlog.android.module)
    alias(libs.plugins.everlog.android.compose)
}

android {
    namespace = "com.everlog.android.common.composetest"
}

// Test support for other modules' tests. It lives in main, with its dependencies as api, because a module's test
// sources can't be shared.

dependencies {
    implementation(projects.android.common.compose)
}
