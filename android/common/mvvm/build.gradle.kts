plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.mvvm"
}

dependencies {
    api(libs.androidx.lifecycle.viewmodel.ktx)
}
