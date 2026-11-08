plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.settings.ui"
}

dependencies {
    implementation(projects.android.settings.data)
    implementation(projects.android.auth.data)
    implementation(libs.rxjava)
}
