plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.plan.data"
}

dependencies {
    testImplementation(projects.android.plan.testFixtures)
}
