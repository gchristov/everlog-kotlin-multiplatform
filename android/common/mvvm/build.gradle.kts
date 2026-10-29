plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.mvvm"
}

dependencies {
    api(projects.android.common.kotlin)
    api(libs.androidx.lifecycle.viewmodel.ktx)
}
