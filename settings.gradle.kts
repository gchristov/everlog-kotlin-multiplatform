pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
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
include(":mobile")
