plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.exercise.feature"
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
    testImplementation(projects.android.common.dataTest)
    testImplementation(projects.android.exercise.testFixtures)
    androidTestImplementation(projects.android.exercise.testFixtures)
}
