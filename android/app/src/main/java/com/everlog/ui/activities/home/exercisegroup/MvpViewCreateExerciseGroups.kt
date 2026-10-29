package com.everlog.ui.activities.home.exercisegroup

import com.everlog.data.model.exercise.ELExerciseGroup
import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.dialog.DialogBuilder.DurationPickerDialogType
import com.everlog.ui.onboarding.ExerciseGroupsOnboardingController
import rx.Observable

interface MvpViewCreateExerciseGroups : BaseActivityMvpView {

    fun onClickSave(): Observable<Void>

    fun onClickAdd(): Observable<Void>

    fun onClickSelectionCancel(): Observable<Void>

    fun onClickSelectionInfo(): Observable<Void>

    fun onClickSelectionLink(): Observable<Void>

    fun onClickSelectionDelete(): Observable<Void>

    fun getOnboardingController(): ExerciseGroupsOnboardingController<*>?

    fun getItemsToEdit(): List<ELExerciseGroup>?

    fun showPickerDuration(value: Int, type: DurationPickerDialogType): Observable<Int>

    fun toggleContextToolbar(visible: Boolean, selectedCount: Int)

    fun scrollToBottom()
}