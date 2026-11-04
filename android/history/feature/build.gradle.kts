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
    implementation(projects.android.history.ui)
    implementation(projects.android.statistics.ui)
    implementation(projects.android.common.mvp)
}
