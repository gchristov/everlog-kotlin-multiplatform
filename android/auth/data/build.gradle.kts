plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.auth.data"
}

dependencies {
    testImplementation(projects.android.auth.testFixtures)
}
