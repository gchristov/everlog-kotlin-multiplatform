plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.navigation"
}

dependencies {
    // Destinations carry the shared models screens are opened with
    api(projects.android.common.data)
}
