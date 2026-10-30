plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.design"
}

dependencies {
    // The parents of the dialog and bottom sheet themes
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}
