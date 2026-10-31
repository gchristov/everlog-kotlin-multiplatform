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
    implementation(projects.android.history.data)
    implementation(projects.android.common.mvp)
    implementation(libs.rxbinding)
    implementation(libs.calendarview)
    implementation(libs.rotate.layout)
    implementation(libs.timber)
    testImplementation(projects.android.history.testFixtures)
    androidTestImplementation(projects.android.history.testFixtures)
}
