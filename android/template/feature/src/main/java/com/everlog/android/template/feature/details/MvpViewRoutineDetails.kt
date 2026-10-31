package com.everlog.android.template.feature.details

import com.everlog.android.common.data.model.ELRoutine
import com.everlog.android.common.mvp.activity.BaseActivityMvpView
import rx.Observable

interface MvpViewRoutineDetails : BaseActivityMvpView {

    fun onClickEdit(): Observable<Void>

    fun onClickDelete(): Observable<Void>

    fun onClickPerform(): Observable<Void>

    fun getItemToEdit(): ELRoutine

    fun loadItemDetails(routine: ELRoutine)

    fun toggleEmptyViewVisible(visible: Boolean)
}