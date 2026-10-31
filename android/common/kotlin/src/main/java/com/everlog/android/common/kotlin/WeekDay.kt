package com.everlog.android.common.kotlin

import org.threeten.bp.LocalDate
import java.io.Serializable

data class WeekDay (

        var day: String? = null,
        var date: LocalDate,
        var active: Boolean? = false

) : Serializable