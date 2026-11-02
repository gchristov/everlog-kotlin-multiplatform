plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.onboarding.data"
}

dependencies {
    // The onboarding builds starter templates from the exercise library and saves them
    implementation(projects.android.exercise.data)
    implementation(projects.android.template.data)
    testImplementation(projects.android.exercise.testFixtures)
    testImplementation(projects.android.template.testFixtures)
    testImplementation(projects.android.onboarding.testFixtures)
}
