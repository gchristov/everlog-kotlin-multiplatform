plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.settings.data"
}

dependencies {
    testImplementation(projects.android.settings.testFixtures)
}
