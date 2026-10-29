plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.onboarding.data"
}

dependencies {
    testImplementation(projects.android.onboarding.testFixtures)
}
