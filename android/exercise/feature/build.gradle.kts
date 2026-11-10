plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.exercise"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.android.common.mvp)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.rxbinding)
    implementation(libs.rotate.layout)
    implementation(libs.gson)
    implementation(libs.timber)
    implementation(projects.android.exercise.data)
    implementation(projects.android.workout.data)
    implementation(projects.android.statistics.ui)
    implementation(projects.android.auth.data)
    implementation(projects.android.settings.data)
    testImplementation(projects.android.common.dataTest)
    testImplementation(projects.android.exercise.testFixtures)
    testImplementation(projects.android.workout.testFixtures)
    testImplementation(projects.android.settings.testFixtures)
}
