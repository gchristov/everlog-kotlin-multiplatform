plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.workout.feature"
}

dependencies {
    implementation(projects.android.workout.data)
    testImplementation(projects.android.workout.testFixtures)
    androidTestImplementation(projects.android.workout.testFixtures)
}
