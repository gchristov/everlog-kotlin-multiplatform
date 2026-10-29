plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.showcase.data"
}

dependencies {
    testImplementation(projects.android.showcase.testFixtures)
}
