plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.history"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.android.settings.data)
    implementation(projects.android.workout.data)
    implementation(projects.android.common.mvp)
    implementation(libs.rxbinding)
    implementation(libs.calendarview)
    implementation(libs.rotate.layout)
    implementation(libs.timber)
}
