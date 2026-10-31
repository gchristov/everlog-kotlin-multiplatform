plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.template.feature"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.android.template.data)
    implementation(projects.android.common.mvp)
    implementation(libs.rxbinding)
    implementation(libs.collapsingtoolbar.subtitle)
    testImplementation(projects.android.template.testFixtures)
    androidTestImplementation(projects.android.template.testFixtures)
}
