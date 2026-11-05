plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.statistics.data"
}

dependencies {
    implementation(projects.android.common.persistence)
    testImplementation(projects.android.common.dataTest)
}
