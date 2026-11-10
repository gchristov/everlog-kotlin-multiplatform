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
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(projects.android.history.data)
    implementation(projects.android.settings.data)
    implementation(projects.android.workout.data)
    implementation(projects.android.common.mvp)
    implementation(libs.calendarview)
    implementation(libs.calendar.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(projects.android.common.dataTest)
    testImplementation(projects.android.settings.testFixtures)
    testImplementation(projects.android.workout.testFixtures)
    implementation(libs.timber)
}
