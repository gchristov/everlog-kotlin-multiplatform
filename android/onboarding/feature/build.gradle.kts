plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.onboarding"
}

dependencies {
    implementation(projects.android.onboarding.data)
    implementation(libs.lottie.compose)
    implementation(libs.coil.compose)
    implementation(libs.timber)
    testImplementation(projects.android.onboarding.testFixtures)
    androidTestImplementation(projects.android.onboarding.testFixtures)
}
