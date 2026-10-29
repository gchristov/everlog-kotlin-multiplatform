plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.auth.feature"
}

dependencies {
    implementation(projects.android.auth.data)
    testImplementation(projects.android.auth.testFixtures)
    androidTestImplementation(projects.android.auth.testFixtures)
}
