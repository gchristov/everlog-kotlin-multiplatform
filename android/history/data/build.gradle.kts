plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.history.data"
}

dependencies {
    testImplementation(projects.android.history.testFixtures)
}
