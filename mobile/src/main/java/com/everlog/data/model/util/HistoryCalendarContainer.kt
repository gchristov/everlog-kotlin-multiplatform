package com.everlog.data.model.util

import com.everlog.data.model.workout.ELWorkout
import com.everlog.ui.fragments.home.activity.history.PresenterHistoryHome
import org.threeten.bp.LocalDate
import timber.log.Timber

data class HistoryCalendarContainer (

        var workoutsMap: Map<String, ELWorkout>? = null,
        var monthlyWorkoutsMap: Map<String, List<ELWorkout>>? = null

) {

    private val TAG = "HistoryCalendarContainer"

    fun containsDate(date: LocalDate): Boolean {
        try {
            val key = PresenterHistoryHome.buildDateDayKey(date)
            return workoutsMap?.containsKey(key) == true
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.tag(TAG).e(e)
        }
        return false
    }

    fun workoutsForDate(date: LocalDate): List<ELWorkout>? {
        try {
            val key = PresenterHistoryHome.buildDateMonthKey(date)
            return monthlyWorkoutsMap?.get(key)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Timber.tag(TAG).e(e)
        }
        return null
    }
}