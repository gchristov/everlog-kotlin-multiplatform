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
    implementation(projects.android.auth.data)
    implementation(projects.android.common.persistence)
    implementation(libs.firebase.storage)
    implementation(libs.gson)
    implementation(libs.rxandroid)
    implementation(libs.timber)
    testImplementation(projects.android.exercise.testFixtures)
}
