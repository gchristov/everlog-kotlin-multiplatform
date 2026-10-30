plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.settings.data"
}

dependencies {
    api(projects.android.common.persistence)
    testImplementation(projects.android.settings.testFixtures)
}
