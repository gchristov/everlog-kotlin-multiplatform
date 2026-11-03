package com.everlog.android.common.navigation

import android.content.Intent
import com.everlog.android.common.data.model.exercise.ELExercise
import com.everlog.android.common.data.model.exercise.ELExerciseGroup

// Which tab an exercise's details open on
enum class ExerciseTab {
    Info,
    History,
}

data class ExerciseDetails(val exercise: ELExercise, val tab: ExerciseTab = ExerciseTab.Info) : Destination

// Picks exercises, handed back as exercise groups
data object ExercisePicker : ResultDestination<List<ELExerciseGroup>>, ScreenResult<List<ELExerciseGroup>> {
    override val screenResult get() = this

    override val requestCode = 1

    override fun read(data: Intent?): List<ELExerciseGroup>? = data.serializable(NavigationExtras.EXERCISE_GROUPS)
}
