plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.data"
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(projects.android.common.kotlin)
    api(projects.android.common.firebase)
    api(projects.android.common.datasync)
    api(projects.android.common.persistence)
    api(libs.eventbus)
    api(libs.rxjava)
    implementation(libs.commons.lang3)
    implementation(libs.timber)
    testImplementation(projects.android.common.dataTest)
}
