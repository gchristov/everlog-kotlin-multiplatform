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
    // The template editor shows library exercises
    api(projects.android.exercise.data)
    api(projects.android.common.session)
    implementation(projects.android.plan.data)
    implementation(libs.commons.lang3)
    implementation(libs.timber)
    testImplementation(projects.android.exercise.testFixtures)
    testImplementation(projects.android.template.testFixtures)
}
