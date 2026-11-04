plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.home.data"
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(projects.android.common.persistence)
    testImplementation(libs.gson)
    testImplementation(projects.android.common.dataTest)
}
