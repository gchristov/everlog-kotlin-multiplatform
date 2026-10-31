plugins {
    alias(libs.plugins.everlog.android.module)
    alias(libs.plugins.everlog.android.compose)
}

android {
    namespace = "com.everlog.android.common.sessionui"
}

dependencies {
    api(projects.android.common.session)
    api(projects.android.common.compose)
    testImplementation(projects.android.common.dataTest)
}
