plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.data"
}

dependencies {
    api(projects.android.common.kotlin)
    api(projects.android.common.firebase)
    api(projects.android.common.datasync)
    api(projects.android.common.persistence)
}
