plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.onboarding.feature"
}

dependencies {
    implementation(projects.android.onboarding.data)
    testImplementation(projects.android.onboarding.testFixtures)
    androidTestImplementation(projects.android.onboarding.testFixtures)
}
