package com.everlog.ui.fragments.home.exercise.history

import com.everlog.ui.fragments.home.exercise.BaseMvpViewExerciseTab

interface MvpViewExerciseHistory : BaseMvpViewExerciseTab {

    fun toggleEmptyState(visible: Boolean)
}