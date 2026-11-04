plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.workoutbuilder.data"
}

dependencies {
    implementation(libs.commons.lang3)
    testImplementation(projects.android.common.dataTest)
    testImplementation(libs.gson)
}
