plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.auth.data"
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(libs.firebase.auth)
    api(projects.android.pro.data)
    testImplementation(projects.android.auth.testFixtures)
}
