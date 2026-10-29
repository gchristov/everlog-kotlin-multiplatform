plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.exercise.testfixtures"
}

// Test support for other modules' tests. It lives in main, with its dependencies as api, because a module's test
// sources can't be shared.

dependencies {
    api(projects.android.exercise.data)
    api(projects.android.common.test)
}
