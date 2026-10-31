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
    // The plan screens read the user, their settings and templates through their data module
    api(projects.android.template.data)
    implementation(projects.android.common.persistence)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.commons.lang3)
    implementation(libs.gson)
    implementation(libs.rxandroid)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.retrofit.rxjava)
    implementation(libs.okhttp.logging)
    implementation(libs.timber)
    testImplementation(projects.android.plan.testFixtures)
}
