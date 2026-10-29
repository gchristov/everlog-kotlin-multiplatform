plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.firebase"
}

dependencies {
    api(projects.android.common.kotlin)
}
