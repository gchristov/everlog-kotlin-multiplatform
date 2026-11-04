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
    implementation(projects.android.settings.data)
    implementation(projects.android.workout.data)
    implementation(projects.android.common.mvp)
    implementation(libs.rxbinding)
    implementation(libs.rotate.layout)
}
