plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.plan.ui"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.android.plan.data)
    implementation(projects.android.auth.data)
    implementation(projects.android.common.mvp)
}
