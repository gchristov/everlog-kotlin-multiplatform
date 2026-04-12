package com.everlog.ui.fragments.home.activity.history

import com.everlog.ui.fragments.base.BaseFragmentMvpView
import org.threeten.bp.LocalDate
import rx.Observable
import java.util.*

interface MvpViewHistoryHome : BaseFragmentMvpView {

    fun onViewTypeChanged(): Observable<HistoryHomeFragment.ViewType>

    fun onDateSelected(): Observable<LocalDate>

    fun refreshCalendar(minDate: Date?)

    fun toggleEmptyState(visible: Boolean)

    fun setViewType(type: HistoryHomeFragment.ViewType)

    fun scrollToDay(index: Int)
}