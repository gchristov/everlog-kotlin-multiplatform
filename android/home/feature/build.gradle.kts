plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.home"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.android.home.data)
    implementation(projects.android.history.ui)
    implementation(projects.android.statistics.ui)
    implementation(projects.android.plan.data)
    implementation(projects.android.plan.ui)
    implementation(projects.android.auth.data)
    implementation(projects.android.settings.data)
    implementation(projects.android.workout.data)
    implementation(projects.android.common.mvp)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.rxbinding)
    implementation(libs.timber)
    implementation(libs.circular.progress.view) {
        isTransitive = false
    }
    implementation(libs.maskable.layout) {
        isTransitive = false
    }
    testImplementation(projects.android.common.dataTest)
    testImplementation(projects.android.settings.testFixtures)
    testImplementation(projects.android.workout.testFixtures)
    testImplementation(projects.android.plan.testFixtures)
    androidTestImplementation(projects.android.settings.testFixtures)
    androidTestImplementation(projects.android.workout.testFixtures)
    androidTestImplementation(projects.android.plan.testFixtures)
}
