plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.exercise.feature"
}

dependencies {
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.gson)
    implementation(libs.timber)
    implementation(projects.android.exercise.data)
    testImplementation(projects.android.exercise.testFixtures)
    androidTestImplementation(projects.android.exercise.testFixtures)
}
