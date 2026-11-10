plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.template"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.android.template.data)
    implementation(projects.android.exercise.data)
    implementation(projects.android.settings.data)
    implementation(projects.android.common.mvp)
    implementation(projects.android.workoutBuilder.ui)
    implementation(libs.rxbinding)
    implementation(libs.collapsingtoolbar.subtitle)
    testImplementation(projects.android.template.testFixtures)
}
