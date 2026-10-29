plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins.register("android-base-plugin") {
        id = "android-base-plugin"
        implementationClass = "com.everlog.gradleplugins.android.AndroidBasePlugin"
    }
    plugins.register("android-module-plugin") {
        id = "android-module-plugin"
        implementationClass = "com.everlog.gradleplugins.android.AndroidModulePlugin"
    }
    plugins.register("android-compose-plugin") {
        id = "android-compose-plugin"
        implementationClass = "com.everlog.gradleplugins.android.AndroidComposePlugin"
    }
    plugins.register("android-firebase-plugin") {
        id = "android-firebase-plugin"
        implementationClass = "com.everlog.gradleplugins.android.AndroidFirebasePlugin"
    }
    plugins.register("android-feature-plugin") {
        id = "android-feature-plugin"
        implementationClass = "com.everlog.gradleplugins.android.AndroidFeaturePlugin"
    }
    plugins.register("android-data-plugin") {
        id = "android-data-plugin"
        implementationClass = "com.everlog.gradleplugins.android.AndroidDataPlugin"
    }
    plugins.register("android-application-binary-plugin") {
        id = "android-application-binary-plugin"
        implementationClass = "com.everlog.gradleplugins.android.AndroidApplicationBinaryPlugin"
    }
}

dependencies {
    // On the classpath so the convention plugins can apply them by id
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kotlin.compose.gradlePlugin)
    implementation(libs.google.services.gradlePlugin)
    implementation(libs.firebase.crashlytics.gradlePlugin)
    implementation(libs.firebase.perf.gradlePlugin)
}
