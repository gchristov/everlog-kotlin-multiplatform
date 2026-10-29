plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.template.data"
}

dependencies {
    testImplementation(projects.android.template.testFixtures)
}
