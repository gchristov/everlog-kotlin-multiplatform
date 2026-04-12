package com.everlog.data.controllers.statistics

import com.everlog.data.model.set.ELSet
import com.everlog.data.model.workout.ELWorkout
import com.everlog.ui.fragments.home.activity.statistics.StatisticsHomeFragment
import com.everlog.ui.fragments.home.activity.statistics.charts.axis.DateAxisFormatter
import com.everlog.ui.fragments.home.activity.statistics.charts.axis.MonthAxisFormatter
import com.everlog.ui.fragments.home.activity.statistics.charts.axis.YearAxisFormatter
import com.everlog.utils.ArrayResourceTypeUtils
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

abstract class BaseStatsController {

    companion object {

        fun chartGranularity(): Float {
            return 1F // Because we're using days
        }

        fun chartAxisFormatter(range: StatisticsHomeFragment.RangeType): IAxisValueFormatter {
            return when (range) {
                StatisticsHomeFragment.RangeType.YEAR -> MonthAxisFormatter()
                StatisticsHomeFragment.RangeType.OVERALL -> YearAxisFormatter()
                else -> DateAxisFormatter()
            }
        }

        fun calculate1RM(maxWeightSet: ELSet?): Float {
            // Brzycki - https://en.wikipedia.org/wiki/One-repetition_maximum
            val w = maxWeightSet?.getWeight() ?: 0f
            val r = maxWeightSet?.getReps()?.toFloat() ?: 0f
            // w and r could be -1, depending on set, as they are the default values
            return max(0f, w / (1.0278f - 0.0278f * r))
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
                                    range: StatisticsHomeFragment.RangeType) {
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
                                             range: StatisticsHomeFragment.RangeType) {
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
                                             range: StatisticsHomeFragment.RangeType) {
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
                                        range: StatisticsHomeFragment.RangeType) {
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

    private fun getChartXAndDividerKey(workout: ELWorkout, range: StatisticsHomeFragment.RangeType): Pair<Long, Float> {
        val workoutDate: Long
        val x: Float
        when (range) {
            StatisticsHomeFragment.RangeType.YEAR -> {
                workoutDate = workout.getCompletedDateWithMonthOnlyTimestamp()
                x = workout.getMonth().toFloat()
            }
            StatisticsHomeFragment.RangeType.OVERALL -> {
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
