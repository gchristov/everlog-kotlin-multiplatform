package com.everlog.utils

class UnitUtils {

    companion object {

        private const val KG_TO_LB_RATIO = 2.20462262185f

        fun kgToLb(kg: Float): Float {
            return kg * KG_TO_LB_RATIO
        }

        fun lbToKg(lb: Float): Float {
            return lb / KG_TO_LB_RATIO
        }
    }
}