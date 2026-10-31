plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.settings"
}

dependencies {
    implementation(projects.android.settings.data)
    testImplementation(projects.android.settings.testFixtures)
    androidTestImplementation(projects.android.settings.testFixtures)
}
