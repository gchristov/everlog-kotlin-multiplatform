package com.everlog.android.exercise.feature.details.history

import com.everlog.android.exercise.feature.details.BaseMvpViewExerciseTab

interface MvpViewExerciseHistory : BaseMvpViewExerciseTab {

    fun toggleEmptyState(visible: Boolean)
}