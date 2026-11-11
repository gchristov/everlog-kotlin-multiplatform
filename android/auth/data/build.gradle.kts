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
    // ELUser holds the Pro subscription
    api(projects.android.pro.data)
    // LocalUserManager is a PreferencesManager
    api(projects.android.common.persistence)
    implementation(libs.gson)
}
