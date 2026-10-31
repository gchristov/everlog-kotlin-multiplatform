plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.history"
}

dependencies {
    implementation(projects.android.history.data)
    testImplementation(projects.android.history.testFixtures)
    androidTestImplementation(projects.android.history.testFixtures)
}
