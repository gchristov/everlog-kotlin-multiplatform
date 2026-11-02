package com.everlog.application.features

import com.everlog.android.exercise.ExerciseConfig
import com.everlog.managers.RemoteConfigManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@ContributesBinding(AppScope::class)
@Inject
class AppExerciseConfig : ExerciseConfig {
    override fun exerciseDetailsV2(): Boolean = RemoteConfigManager.manager.exerciseDetailsV2()
}
