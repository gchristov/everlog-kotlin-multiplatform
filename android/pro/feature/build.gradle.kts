plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.pro"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.android.pro.data)
    implementation(projects.android.common.mvp)
    implementation(libs.billing)
    implementation(libs.rxbinding)
    implementation(libs.timber)
    implementation(libs.shapeOfView)
    testImplementation(projects.android.pro.testFixtures)
    androidTestImplementation(projects.android.pro.testFixtures)
}
