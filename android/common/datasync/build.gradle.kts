plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.datasync"
}

dependencies {
    api(projects.android.common.kotlin)
    api(projects.android.common.firebase)
}
