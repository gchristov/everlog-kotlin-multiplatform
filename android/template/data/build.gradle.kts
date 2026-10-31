plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.template.data"
}

dependencies {
    // The template screens read the user and their settings through their data module
    api(projects.android.auth.data)
    api(projects.android.settings.data)
    implementation(projects.android.exercise.data)
    implementation(libs.timber)
    testImplementation(projects.android.template.testFixtures)
}
