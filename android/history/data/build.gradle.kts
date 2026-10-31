plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.history.data"
}

dependencies {
    // The history screens read workouts, the user and their settings through their data module
    api(projects.android.workout.data)
    testImplementation(projects.android.history.testFixtures)
}
