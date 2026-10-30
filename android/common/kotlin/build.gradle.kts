plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.kotlin"
}

dependencies {
    api(libs.kotlinx.coroutines.android)
    api(libs.arrow.core)
    api(libs.threetenabp)
    implementation(libs.commons.lang3)
    implementation(libs.timber)
}
