plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.template.feature"
}

dependencies {
    implementation(projects.android.template.data)
    testImplementation(projects.android.template.testFixtures)
    androidTestImplementation(projects.android.template.testFixtures)
}
