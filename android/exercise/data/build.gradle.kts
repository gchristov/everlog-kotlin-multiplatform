plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.exercise.data"
}

dependencies {
    testImplementation(projects.android.exercise.testFixtures)
}
