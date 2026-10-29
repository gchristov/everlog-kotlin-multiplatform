plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.exercise.feature"
}

dependencies {
    implementation(projects.android.exercise.data)
    testImplementation(projects.android.exercise.testFixtures)
    androidTestImplementation(projects.android.exercise.testFixtures)
}
