plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.onboarding.data"
}

dependencies {
    // The onboarding builds starter templates from the exercise library and saves them
    api(projects.android.template.data)
    testImplementation(projects.android.onboarding.testFixtures)
}
