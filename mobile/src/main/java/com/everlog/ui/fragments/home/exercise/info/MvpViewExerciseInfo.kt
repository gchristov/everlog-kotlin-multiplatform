package com.everlog.ui.fragments.home.exercise.info

import com.everlog.ui.fragments.home.exercise.BaseMvpViewExerciseTab
import rx.Observable

interface MvpViewExerciseInfo : BaseMvpViewExerciseTab {

    fun onClickYoutube(): Observable<Void>
}