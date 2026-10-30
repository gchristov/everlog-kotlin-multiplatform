plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.plan.data"
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(libs.commons.lang3)
    testImplementation(projects.android.plan.testFixtures)
}
