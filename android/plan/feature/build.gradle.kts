plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.plan"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.android.plan.data)
    implementation(projects.android.common.mvp)
    implementation(libs.rxbinding)
    implementation(libs.timber)
    implementation(libs.aspect)
    implementation(libs.collapsingtoolbar.subtitle)
    implementation(libs.scrollingpagerindicator)
    testImplementation(projects.android.plan.testFixtures)
    androidTestImplementation(projects.android.plan.testFixtures)
}
