package com.everlog.android.history.workouts

import com.everlog.android.common.mvp.fragment.BaseFragmentMvpView
import java.util.*
import org.threeten.bp.LocalDate
import rx.Observable

interface MvpViewHistoryHome : BaseFragmentMvpView {

    fun onViewTypeChanged(): Observable<HistoryHomeFragment.ViewType>

    fun onDateSelected(): Observable<LocalDate>

    fun refreshCalendar(minDate: Date?)

    fun toggleEmptyState(visible: Boolean)

    fun setViewType(type: HistoryHomeFragment.ViewType)

    fun scrollToDay(index: Int)
}