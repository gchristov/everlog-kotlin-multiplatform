plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.session"
}

dependencies {
    api(projects.android.common.kotlin)
    api(projects.android.common.data)
    implementation(libs.commons.lang3)
    testImplementation(projects.android.common.dataTest)
    testImplementation(libs.gson)
}
