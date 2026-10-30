plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.mvp"
}

dependencies {
    api(projects.android.common.kotlin)
    api(projects.android.common.design)
    api(libs.androidx.recyclerview)
    api(libs.androidx.appcompat)
    api(libs.material)
    api(libs.androidx.activity)
    api(libs.androidx.localbroadcastmanager)
    api(libs.eventbus)
    api(libs.rxjava)
    api(libs.rxandroid)
    api(libs.shimmer)
    implementation(libs.timber)
}
