package com.everlog.ui.activities.home.exercise

import com.everlog.ui.activities.base.BaseActivityMvpView
import com.everlog.ui.dialog.DialogBuilder
import com.everlog.ui.onboarding.ExerciseGroupsOnboardingController
import com.everlog.ui.onboarding.ExercisesOnboardingController
import com.everlog.ui.views.revealcircle.FilterExercisesView
import rx.Observable

interface MvpViewExercises : BaseActivityMvpView {

    fun onSearchChanged(): Observable<String>

    fun onClickAdd(): Observable<Void>

    fun onClickEmptyAction(): Observable<Void>

    fun onClickSelectionLink(): Observable<Void>

    fun onClickSelectionAdd(): Observable<Void>

    fun onSearchHidden(): Observable<Void>

    fun onFiltersClick(): Observable<Void>

    fun onFiltersChanged(): Observable<FilterExercisesView.ExerciseFilters>

    fun isSelectionMode(): Boolean

    fun stopSearch()

    fun toggleEmptyState(visible: Boolean, searchText: String?)

    fun toggleAddBtn(visible: Boolean, selectionCount: Int)

    fun toggleFilters(filters: FilterExercisesView.ExerciseFilters)

    fun scrollToTop()

    fun getOnboardingController(): ExercisesOnboardingController?

    fun getScrollPosition(): Int

    fun setScrollPosition(position: Int)

    fun showPickerMultipleChoice(values: Array<String>, selectedIndex: Int, type: DialogBuilder.MultipleChoiceDialogType): Observable<Int>
}