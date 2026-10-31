plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.workout.feature"
}

dependencies {
    implementation(projects.android.workout.data)
    implementation(projects.android.common.mvp)
    implementation(projects.android.common.sessionUi)
    implementation(libs.timber)
    testImplementation(projects.android.workout.testFixtures)
    androidTestImplementation(projects.android.workout.testFixtures)
}
