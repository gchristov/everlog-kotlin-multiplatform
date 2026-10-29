plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.showcase.feature"
}

dependencies {
    implementation(projects.android.showcase.data)
    testImplementation(projects.android.showcase.testFixtures)
    androidTestImplementation(projects.android.showcase.testFixtures)
}
