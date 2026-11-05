plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.history.data"
}

dependencies {
    implementation(projects.android.common.persistence)
}
