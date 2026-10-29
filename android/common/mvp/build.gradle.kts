plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.mvp"
}

dependencies {
    api(projects.android.common.kotlin)
    api(projects.android.common.design)
}
