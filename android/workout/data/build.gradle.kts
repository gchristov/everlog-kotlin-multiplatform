plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.workout.data"
}

dependencies {
    implementation(projects.android.auth.data)
    implementation(projects.android.settings.data)
    implementation(projects.android.template.data)
    implementation(projects.android.common.persistence)
    implementation(libs.gson)
    implementation(libs.rxandroid)
    implementation(libs.timber)
    testImplementation(projects.android.common.dataTest)
    testImplementation(projects.android.settings.testFixtures)
    testImplementation(projects.android.workout.testFixtures)
}
