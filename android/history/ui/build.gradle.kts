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
    implementation(libs.timber)
}
