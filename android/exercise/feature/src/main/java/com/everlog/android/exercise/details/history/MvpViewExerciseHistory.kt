package com.everlog.android.exercise.details.history

import com.everlog.android.exercise.details.BaseMvpViewExerciseTab

interface MvpViewExerciseHistory : BaseMvpViewExerciseTab {

    fun toggleEmptyState(visible: Boolean)
}