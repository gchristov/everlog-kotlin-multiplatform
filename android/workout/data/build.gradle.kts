plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.workout.data"
}

dependencies {
    implementation(projects.android.auth.data)
    testImplementation(projects.android.common.dataTest)
    testImplementation(projects.android.workout.testFixtures)
}
