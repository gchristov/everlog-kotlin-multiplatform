package com.everlog.data.model

import org.threeten.bp.LocalDate
import java.io.Serializable

data class WeekDay (

        var day: String? = null,
        var date: LocalDate,
        var active: Boolean? = false

) : Serializable