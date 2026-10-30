package com.everlog.ui.activities.home.routine.create

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.ui.activities.home.exercisegroup.MvpViewCreateExerciseGroups
import rx.Observable

interface MvpViewCreateRoutine : MvpViewCreateExerciseGroups {

    fun showNamePrompt(name: String?): Observable<String>

    fun getItemToEdit(): ELRoutine?

    fun shouldOpenDetailsOnSuccess(): Boolean
}