plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.template.data"
}

dependencies {
    implementation(projects.android.auth.data)
    implementation(projects.android.exercise.data)
    implementation(libs.timber)
    testImplementation(projects.android.template.testFixtures)
}
