plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.statistics.ui"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.android.statistics.data)
    implementation(projects.android.settings.data)
    implementation(projects.android.workout.data)
    implementation(projects.android.common.mvp)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(projects.android.common.dataTest)
    testImplementation(projects.android.settings.testFixtures)
    testImplementation(projects.android.workout.testFixtures)
    androidTestImplementation(projects.android.settings.testFixtures)
    androidTestImplementation(projects.android.workout.testFixtures)
    implementation(libs.rxbinding)
    implementation(libs.rotate.layout)
}
