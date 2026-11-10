plugins {
    alias(libs.plugins.everlog.android.data)
}

android {
    namespace = "com.everlog.android.home.data"
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(projects.android.common.persistence)
    implementation(projects.android.exercise.data)
    implementation(projects.android.plan.data)
    implementation(projects.android.template.data)
    implementation(projects.android.workout.data)
    implementation(libs.commons.lang3)
    testImplementation(libs.gson)
    testImplementation(projects.android.common.dataTest)
    testImplementation(projects.android.exercise.testFixtures)
    testImplementation(projects.android.plan.testFixtures)
    testImplementation(projects.android.template.testFixtures)
    testImplementation(projects.android.workout.testFixtures)
}
