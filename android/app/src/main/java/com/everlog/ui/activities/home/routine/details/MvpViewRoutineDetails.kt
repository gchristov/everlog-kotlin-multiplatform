package com.everlog.ui.activities.home.routine.details

import com.everlog.data.model.ELRoutine
import com.everlog.ui.activities.base.BaseActivityMvpView
import rx.Observable

interface MvpViewRoutineDetails : BaseActivityMvpView {

    fun onClickEdit(): Observable<Void>

    fun onClickDelete(): Observable<Void>

    fun onClickPerform(): Observable<Void>

    fun getItemToEdit(): ELRoutine

    fun loadItemDetails(routine: ELRoutine)

    fun toggleEmptyViewVisible(visible: Boolean)
}