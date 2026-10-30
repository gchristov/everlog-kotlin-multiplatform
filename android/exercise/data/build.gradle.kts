plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.exercise.data"
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    testImplementation(projects.android.exercise.testFixtures)
}
