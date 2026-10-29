plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.plan.feature"
}

dependencies {
    implementation(projects.android.plan.data)
    testImplementation(projects.android.plan.testFixtures)
    androidTestImplementation(projects.android.plan.testFixtures)
}
