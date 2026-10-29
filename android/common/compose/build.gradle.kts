plugins {
    alias(libs.plugins.everlog.android.module)
    alias(libs.plugins.everlog.android.compose)
}

android {
    namespace = "com.everlog.android.common.compose"
}

dependencies {
    api(projects.android.common.design)
}
