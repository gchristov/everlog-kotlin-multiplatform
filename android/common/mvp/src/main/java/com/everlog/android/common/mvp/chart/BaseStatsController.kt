package com.everlog.android.common.mvp.chart

import com.everlog.android.common.data.model.workout.ELWorkout
import com.everlog.android.common.data.model.workout.RangeType
import com.everlog.android.common.mvp.chart.axis.DateAxisFormatter
import com.everlog.android.common.mvp.chart.axis.MonthAxisFormatter
import com.everlog.android.common.mvp.chart.axis.YearAxisFormatter
import com.everlog.android.common.kotlin.format.ArrayResourceTypeUtils
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

abstract class BaseStatsController {

    companion object {

        fun chartGranularity(): Float {
            return 1F // Because we're using days
        }

        fun chartAxisFormatter(range: RangeType): ValueFormatter {
            return when (range) {
                RangeType.YEAR -> MonthAxisFormatter()
                RangeType.OVERALL -> YearAxisFormatter()
                else -> DateAxisFormatter()
            }
        }
    }

    protected fun calculateCategoryCounts(currentValues: MutableMap<String, PieEntry>, newValues: Map<String, Int>) {
        for (category in newValues.keys) {
            var currentEntry = currentValues[category]
            var newValue = newValues[category]
            if (currentEntry == null) {
                val categoryName = ArrayResourceTypeUtils.withExerciseCategories().getTitle(category, category.lowercase().capitalize())
                currentEntry = PieEntry(0F, categoryName, null)
            }
            if (newValue == null) {
                newValue = 0
            }
            currentEntry.y += newValue
            currentValues[category] = currentEntry
        }
    }

    protected fun calculateMaxValue(workout: ELWorkout,
                                    currentValues: MutableMap<Long, Entry>,
                                    newValue: Float,
                                    range: RangeType) {
        val keys = getChartXAndDividerKey(workout, range)
        var currentEntry = currentValues[keys.first]
        if (currentEntry == null) {
            currentEntry = Entry(keys.second, 0f)
        }
        // Set max weight for that day
        currentEntry.y = max(newValue, currentEntry.y)
        currentValues[keys.first] = currentEntry
    }

    protected fun calculateAccumulativeValue(workout: ELWorkout,
                                             currentValues: MutableMap<Long, Entry>,
                                             newValue: Int,
                                             range: RangeType) {
        val keys = getChartXAndDividerKey(workout, range)
        var currentEntry = currentValues[keys.first]
        if (currentEntry == null) {
            currentEntry = Entry(keys.second, 0f)
        }
        currentEntry.y += newValue
        currentValues[keys.first] = currentEntry
    }

    protected fun calculateAccumulativeValue(workout: ELWorkout,
                                             currentValues: MutableMap<Long, BarEntry>,
                                             newValue: Float,
                                             range: RangeType) {
        val keys = getChartXAndDividerKey(workout, range)
        var currentEntry = currentValues[keys.first]
        if (currentEntry == null) {
            currentEntry = BarEntry(keys.second, 0f)
        }
        currentEntry.y += newValue
        currentValues[keys.first] = currentEntry
    }

    protected fun calculateAverageValue(workout: ELWorkout,
                                        currentValues: MutableMap<Long, Entry>,
                                        currentValuesPerTimePeriod: MutableMap<Int, MutableList<Float>>,
                                        newValue: Float,
                                        range: RangeType) {
        val keys = getChartXAndDividerKey(workout, range)
        var currentEntry = currentValues[keys.first]
        if (currentEntry == null) {
            currentEntry = Entry(keys.second, 0f)
        }
        var valuesToSum = currentValuesPerTimePeriod[keys.second.toInt()]
        if (valuesToSum == null) {
            valuesToSum = ArrayList()
        }
        valuesToSum.add(newValue)
        currentEntry.y = valuesToSum.sum() / valuesToSum.size
        currentValues[keys.first] = currentEntry
        currentValuesPerTimePeriod[keys.second.toInt()] = valuesToSum
    }

    private fun getChartXAndDividerKey(workout: ELWorkout, range: RangeType): Pair<Long, Float> {
        val workoutDate: Long
        val x: Float
        when (range) {
            RangeType.YEAR -> {
                workoutDate = workout.getCompletedDateWithMonthOnlyTimestamp()
                x = workout.getMonth().toFloat()
            }
            RangeType.OVERALL -> {
                workoutDate = workout.getCompletedDateWithYearOnlyTimestamp()
                x = workout.getYear().toFloat()
            }
            else -> {
                workoutDate = workout.getCompletedDateWithNoHourTimestamp()
                x = TimeUnit.MILLISECONDS.toDays(workoutDate).toFloat()
            }
        }
        return Pair(workoutDate, x)
    }
}
