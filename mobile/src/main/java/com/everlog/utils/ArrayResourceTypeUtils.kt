package com.everlog.utils

import android.content.Context
import com.everlog.R
import com.everlog.application.ELApplication.Companion.getInstance
import java.util.*

class ArrayResourceTypeUtils private constructor(context: Context,
                                                 arrayTypeResId: Int,
                                                 arrayTitlesResId: Int) {

    val types: Array<String> = context.resources.getStringArray(arrayTypeResId)
    val titles: Array<String> = context.resources.getStringArray(arrayTitlesResId)

    companion object {

        private val cache: MutableMap<Int, ArrayResourceTypeUtils?> = HashMap()

        fun withSetTypes(): ArrayResourceTypeUtils {
            return with(R.array.set_types, R.array.set_type_titles)
        }

        @JvmStatic
        fun withExerciseCategories(): ArrayResourceTypeUtils {
            return with(R.array.exercise_categories, R.array.exercise_category_titles)
        }

        fun withWeightUnits(): ArrayResourceTypeUtils {
            return with(R.array.units_weight, R.array.units_weight_titles)
        }

        @JvmStatic
        fun withWeightAbbreviations(): ArrayResourceTypeUtils {
            return with(R.array.units_weight, R.array.units_weight_abbreviations)
        }

        fun with(arrayTypeResId: Int, arrayTitlesResId: Int): ArrayResourceTypeUtils {
            val key = arrayTitlesResId + arrayTitlesResId
            return if (cache.containsKey(key)) {
                cache[key]!!
            } else {
                val utils = ArrayResourceTypeUtils(getInstance(), arrayTypeResId, arrayTitlesResId)
                cache[key] = utils
                utils
            }
        }
    }

    fun getTitle(type: String, defaultTitle: String): String? {
        val index = getTypeIndex(type)
        if (index < 0) {
            return defaultTitle
        }
        return titles[index]
    }

    fun getType(index: Int): String? {
        return if (index >= 0 && index < types.size) {
            types[index]
        } else null
    }

    fun getTypeIndex(type: String): Int {
        return types.indexOf(type)
    }
}