plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.settings.ui"
}

dependencies {
    implementation(projects.android.settings.data)
    implementation(projects.android.auth.data)
    implementation(projects.android.common.mvp)
    implementation(libs.rxjava)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.localbroadcastmanager)
    testImplementation(projects.android.settings.testFixtures)
    testImplementation(projects.android.auth.testFixtures)
    androidTestImplementation(projects.android.settings.testFixtures)
    androidTestImplementation(projects.android.auth.testFixtures)
}
