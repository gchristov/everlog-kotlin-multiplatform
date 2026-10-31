plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.workout.data"
}

dependencies {
    // The workout screens read templates, exercises, the user and their settings through their data module
    api(projects.android.template.data)
    implementation(projects.android.common.persistence)
    implementation(libs.gson)
    implementation(libs.rxandroid)
    implementation(libs.timber)
    testImplementation(projects.android.common.dataTest)
    testImplementation(projects.android.workout.testFixtures)
}
