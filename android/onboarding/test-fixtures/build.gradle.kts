plugins {
    alias(libs.plugins.everlog.android.module)
}

android {
    namespace = "com.everlog.android.onboarding.testfixtures"
}

// Test support for other modules' tests. It lives in main, with its dependencies as api, because a module's test
// sources can't be shared.

dependencies {
    api(projects.android.onboarding.data)
    api(projects.android.common.test)
    // The onboarding's tests run its use cases on the exercise and template fakes
    api(projects.android.exercise.testFixtures)
    api(projects.android.template.testFixtures)
}
