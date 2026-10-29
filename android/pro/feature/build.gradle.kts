plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.pro.feature"
}

dependencies {
    implementation(projects.android.pro.data)
    testImplementation(projects.android.pro.testFixtures)
    androidTestImplementation(projects.android.pro.testFixtures)
}
