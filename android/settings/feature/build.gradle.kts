plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.settings"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.android.settings.data)
    implementation(projects.android.common.mvp)
    implementation(libs.rxbinding)
    implementation(libs.timber)
    implementation(libs.roundedimageview)
    testImplementation(projects.android.settings.testFixtures)
    androidTestImplementation(projects.android.settings.testFixtures)
}
