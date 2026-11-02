plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.settings.data"
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(projects.android.common.persistence)
    implementation(projects.android.auth.data)
    testImplementation(projects.android.settings.testFixtures)
}
