plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.workout"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.android.workout.data)
    implementation(projects.android.exercise.data)
    implementation(projects.android.settings.data)
    implementation(projects.android.template.data)
    implementation(projects.android.common.mvp)
    implementation(projects.android.common.sessionUi)
    implementation(libs.timber)
    implementation(libs.commons.lang3)
    implementation(libs.konfetti)
    implementation(libs.achievementView)
    implementation(libs.collapsingtoolbar.subtitle)
    testImplementation(projects.android.common.dataTest)
    androidTestImplementation(projects.android.settings.testFixtures)
}
