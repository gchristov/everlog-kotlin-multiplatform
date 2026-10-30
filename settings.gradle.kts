enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("gradle-plugins")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.develocity") version "4.4.0"
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/terms-of-service")
        termsOfUseAgree.set("yes")
        publishing.onlyIf { true }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://clojars.org/repo/") }
        // Aliyun mirror often contains legacy JCenter/Bintray artifacts
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "everlog-kotlin-multiplatform"

include(":android:app")
include(":android:common:compose")
include(":android:common:compose-test")
include(":android:common:data")
include(":android:common:data-test")
include(":android:common:datasync")
include(":android:common:design")
include(":android:common:firebase")
include(":android:common:kotlin")
include(":android:common:mvp")
include(":android:common:mvvm")
include(":android:common:persistence")
include(":android:common:test")
include(":android:auth:data")
include(":android:auth:feature")
include(":android:auth:test-fixtures")
include(":android:exercise:data")
include(":android:exercise:feature")
include(":android:exercise:test-fixtures")
include(":android:history:data")
include(":android:history:feature")
include(":android:history:test-fixtures")
include(":android:onboarding:data")
include(":android:onboarding:feature")
include(":android:onboarding:test-fixtures")
include(":android:plan:data")
include(":android:plan:feature")
include(":android:plan:test-fixtures")
include(":android:pro:data")
include(":android:pro:feature")
include(":android:pro:test-fixtures")
include(":android:settings:data")
include(":android:settings:feature")
include(":android:settings:test-fixtures")
include(":android:showcase:data")
include(":android:showcase:feature")
include(":android:showcase:test-fixtures")
include(":android:template:data")
include(":android:template:feature")
include(":android:template:test-fixtures")
include(":android:workout:data")
include(":android:workout:feature")
include(":android:workout:test-fixtures")
