plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.history.ui"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.android.settings.data)
    implementation(projects.android.workout.data)
    implementation(projects.android.common.mvp)
    implementation(libs.calendarview)
    implementation(libs.calendar.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(projects.android.common.dataTest)
    testImplementation(projects.android.settings.testFixtures)
    testImplementation(projects.android.workout.testFixtures)
    androidTestImplementation(projects.android.settings.testFixtures)
    androidTestImplementation(projects.android.workout.testFixtures)
    implementation(libs.timber)
}
