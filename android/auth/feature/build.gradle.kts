plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.auth"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.android.auth.data)
    implementation(projects.android.common.mvp)
    implementation(libs.rxbinding)
    implementation(libs.timber)
    implementation(libs.loading.button)
    testImplementation(projects.android.auth.testFixtures)
    androidTestImplementation(projects.android.auth.testFixtures)
}
