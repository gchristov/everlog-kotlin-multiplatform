plugins {
    alias(libs.plugins.everlog.android.module)
    alias(libs.plugins.everlog.android.compose)
}

android {
    namespace = "com.everlog.android.common.compose"
}

dependencies {
    api(projects.android.common.design)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.haze)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
