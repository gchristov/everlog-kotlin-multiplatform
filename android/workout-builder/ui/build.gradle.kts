plugins {
    alias(libs.plugins.everlog.android.feature)
}

android {
    namespace = "com.everlog.android.workoutbuilder.ui"
}

dependencies {
    // The table shows the builder's blocks and sets, opens an exercise on a tab (ExerciseTab) and is built from
    // design system elements, so screens using it see all three
    api(projects.android.workoutBuilder.data)
    api(projects.android.common.compose)
    api(projects.android.common.navigation)
}
