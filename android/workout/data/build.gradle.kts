plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.workout.data"
}

dependencies {
    testImplementation(projects.android.workout.testFixtures)
}
