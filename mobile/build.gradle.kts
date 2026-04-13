import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
}

android {
    namespace = "com.everlog"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.everlog"
        minSdk = 23
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 153
        versionName = "2.8.0"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xexpect-builtin-arguments-junction")
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("../deploy/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = file("../deploy/release_key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEYSTORE_ALIAS")
            keyPassword = System.getenv("KEYSTORE_ALIAS_PASSWORD")
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            versionNameSuffix = " dev"
            isDebuggable = true
            buildConfigField("String", "E2E_TEST_USER_EMAIL", "\"${project.envSecret("E2E_TEST_USER_EMAIL")}\"")
            buildConfigField("String", "E2E_TEST_USER_PASSWORD", "\"${project.envSecret("E2E_TEST_USER_PASSWORD")}\"")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            isDebuggable = false
        }
    }
}

configurations.all {
    resolutionStrategy {
        dependencySubstitution {
            substitute(module("org.hamcrest:hamcrest-core")).using(module("org.hamcrest:hamcrest:3.0"))
            substitute(module("org.hamcrest:hamcrest-library")).using(module("org.hamcrest:hamcrest:3.0"))
        }
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.truth)
    
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
    
    implementation(libs.play.services.auth)
    implementation(libs.play.services.fitness)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.cardview)
    implementation(libs.billing)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.glide)

    implementation(libs.rxandroid)
    implementation(libs.rxjava)
    implementation(libs.rxbinding)

    implementation(libs.kotlin.stdlib)

    implementation(libs.shimmer)

    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.retrofit.rxjava)
    implementation(libs.okhttp.logging)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.calendarview)

    implementation(libs.commons.lang3)
    implementation(libs.datetimeutils)
    
    implementation(libs.loading.button)
    
    implementation(libs.mpandroidchart)
    implementation(libs.nineoldandroids)
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
    implementation(libs.simpleratingbar) {
        isTransitive = false
    }
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

fun Project.envSecret(key: String): String {
    val propFile = file("./secrets.properties")
    val properties = Properties()
    properties.load(FileInputStream(propFile))
    val property = properties.getProperty(key)
    if (property.isNullOrBlank()) {
        throw IllegalStateException("Required property is missing: property=$key")
    }
    return property
}