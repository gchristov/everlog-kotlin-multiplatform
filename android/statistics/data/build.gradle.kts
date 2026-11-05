plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.statistics.data"
}

dependencies {
    testImplementation(projects.android.common.dataTest)
}
