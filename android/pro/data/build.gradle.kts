plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.pro.data"
}

dependencies {
    testImplementation(projects.android.pro.testFixtures)
}
