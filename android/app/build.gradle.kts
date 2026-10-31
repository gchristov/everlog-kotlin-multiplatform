import com.everlog.gradleplugins.envSecret

plugins {
    alias(libs.plugins.everlog.android.applicationBinary)
    alias(libs.plugins.everlog.android.compose)
    alias(libs.plugins.everlog.android.firebase)
}

android {
    namespace = "com.everlog"
    ndkVersion = "26.1.10909125" // Must match version in .github/actions/setup-gradle/action.yml

    defaultConfig {
        applicationId = "com.everlog"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("src/debug/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = file("src/release/release_key.jks")
            storePassword = envSecret("KEYSTORE_RELEASE_PASSWORD")
            keyAlias = envSecret("KEYSTORE_RELEASE_ALIAS")
            keyPassword = envSecret("KEYSTORE_RELEASE_ALIAS_PASSWORD")
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            isDebuggable = true
            buildConfigField("String", "E2E_TEST_USER_EMAIL", "\"${envSecret("E2E_TEST_USER_EMAIL")}\"")
            buildConfigField("String", "E2E_TEST_USER_PASSWORD", "\"${envSecret("E2E_TEST_USER_PASSWORD")}\"")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isDebuggable = false
            ndk {
                debugSymbolLevel = "FULL"
            }
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
            }
        }
    }
}

dependencies {
    implementation(projects.android.common.compose)
    implementation(projects.android.common.data)
    implementation(projects.android.common.datasync)
    implementation(projects.android.common.design)
    implementation(projects.android.common.kotlin)
    implementation(projects.android.common.mvp)
    implementation(projects.android.common.mvvm)
    implementation(projects.android.common.persistence)
    implementation(projects.android.common.session)
    implementation(projects.android.common.sessionUi)
    implementation(projects.android.auth.data)
    implementation(projects.android.exercise.data)
    implementation(projects.android.plan.data)
    implementation(projects.android.pro.data)
    implementation(projects.android.settings.data)
    implementation(projects.android.template.data)
    implementation(projects.android.workout.data)
    implementation(projects.android.auth.feature)
    implementation(projects.android.exercise.feature)
    implementation(projects.android.history.feature)
    implementation(projects.android.onboarding.feature)
    implementation(projects.android.plan.feature)
    implementation(projects.android.pro.feature)
    implementation(projects.android.settings.feature)
    implementation(projects.android.showcase.feature)
    implementation(projects.android.template.feature)
    implementation(projects.android.workout.feature)

    testImplementation(projects.android.common.test)
    testImplementation(projects.android.common.dataTest)
    testImplementation(projects.android.exercise.testFixtures)
    testImplementation(projects.android.settings.testFixtures)
    testImplementation(projects.android.template.testFixtures)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.hamcrest)
    androidTestImplementation(libs.espresso.core)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.inappmessaging.display)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.ndk.crashlytics)

    implementation(libs.play.services.auth)
    implementation(libs.play.services.fitness)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.cardview)
    implementation(libs.billing)
    implementation(libs.play.app.update)
    implementation(libs.play.review)
    implementation(libs.androidx.core.splashscreen)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    androidTestImplementation(projects.android.common.composeTest)
    // The app's own Compose tests run in its process, so the host activity has to be in the app's debug manifest
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.haze)
    implementation(libs.lottie.compose)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.arrow.core)

    implementation(libs.glide)
    implementation(libs.coil.compose)

    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.rxbinding)

    implementation(libs.shimmer)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.retrofit.rxjava)
    implementation(libs.okhttp.logging)

    implementation(libs.calendarview)

    implementation(libs.commons.lang3)
    implementation(libs.datetimeutils)

    implementation(libs.loading.button)

    implementation(libs.mpandroidchart)
    implementation(libs.roundedimageview)
    implementation(libs.threetenabp)
    implementation(libs.materialnumberpicker)
    implementation(libs.taptargetview)

    implementation(libs.maskable.layout) {
        isTransitive = false
    }
    implementation(libs.circular.progress.view) {
        isTransitive = false
    }
    implementation(libs.eventbus)
    implementation(libs.timber)

    implementation(libs.hyperlog)
    implementation(libs.volley)

    implementation(libs.konfetti)
    implementation(libs.collapsingtoolbar.subtitle)
    implementation(libs.aspect)
    implementation(libs.scrollingpagerindicator)

    implementation(libs.flexbox)

    implementation(libs.dexter)
    implementation(libs.textinlineimage)

    implementation(libs.rotate.layout)

    implementation(libs.shapeOfView)
    implementation(libs.ucrop)
    implementation(libs.bottomSheet)
    implementation(libs.blurView)
    implementation(libs.achievementView)
    implementation(libs.textDrawable)
}
