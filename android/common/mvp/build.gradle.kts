plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.common.mvp"
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    api(projects.android.common.kotlin)
    api(projects.android.common.design)
    api(libs.androidx.recyclerview)
    api(libs.androidx.appcompat)
    api(libs.material)
    api(libs.androidx.activity)
    api(libs.androidx.localbroadcastmanager)
    api(libs.eventbus)
    api(libs.rxjava)
    api(libs.rxandroid)
    api(libs.shimmer)
    api(libs.mpandroidchart)
    api(libs.glide)
    api(libs.taptargetview)
    // ExerciseImageView shows ELExercise
    api(projects.android.common.data)
    implementation(libs.textDrawable)
    implementation(libs.ucrop)
    implementation(libs.bottomSheet)
    implementation(libs.textinlineimage)
    implementation(libs.maskable.layout) {
        isTransitive = false
    }
    implementation(projects.android.common.persistence)
    implementation(libs.timber)
}
