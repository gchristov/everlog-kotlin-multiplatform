plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.onboarding"
}

dependencies {
    implementation(projects.android.onboarding.data)
    implementation(projects.android.settings.data)
    implementation(libs.lottie.compose)
    implementation(libs.coil.compose)
    implementation(libs.timber)
    testImplementation(projects.android.exercise.testFixtures)
    testImplementation(projects.android.template.testFixtures)
    testImplementation(projects.android.onboarding.testFixtures)
}
