plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.pro.data"
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(libs.billing)
    testImplementation(projects.android.pro.testFixtures)
}
