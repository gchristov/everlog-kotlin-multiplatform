plugins {
    alias(libs.plugins.everlog.android.module)
    alias(libs.plugins.everlog.android.compose)
}

android {
    namespace = "com.everlog.android.common.composetest"
}

// Test support for other modules' tests: Compose tests on the emulator, and feature tests on the JVM (FeatureTest). It lives in main, with its dependencies as api, because a module's test
// sources can't be shared.

dependencies {
    api(projects.android.common.compose)
    implementation(projects.android.common.kotlin)
    implementation(libs.threetenabp)
    // Feature tests run on the JVM: FeatureTest
    api(libs.robolectric)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.androidx.test.runner)
    api(libs.androidx.test.ext.junit)
    api(libs.espresso.core)
    api(libs.truth)
    // Gives Compose tests an activity to host their content in. Not debugApi: consumers' androidTest wouldn't get it
    api(libs.androidx.compose.ui.test.manifest)
}
