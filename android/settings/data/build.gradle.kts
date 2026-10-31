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
    // The settings screens read the signed-in user through their data module
    api(projects.android.auth.data)
    testImplementation(projects.android.settings.testFixtures)
}
