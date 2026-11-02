plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.template.data"
}

dependencies {
    implementation(projects.android.auth.data)
    // The template editor shows library exercises
    implementation(projects.android.exercise.data)
    // The template session is built from the shared session model
    api(projects.android.common.session)
    implementation(libs.commons.lang3)
    implementation(libs.timber)
    testImplementation(projects.android.exercise.testFixtures)
    testImplementation(projects.android.template.testFixtures)
}
