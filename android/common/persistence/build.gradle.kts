plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.persistence"
}

dependencies {
    api(projects.android.common.kotlin)
}
