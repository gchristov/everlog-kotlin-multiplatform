package com.everlog.utils

import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

fun LocalDate.toJava(): java.time.LocalDate {
    return java.time.LocalDate.ofEpochDay(this.toEpochDay())
}

fun java.time.LocalDate.fromJava(): LocalDate {
    return LocalDate.ofEpochDay(this.toEpochDay())
}

fun DayOfWeek.toJava(): java.time.DayOfWeek {
    return java.time.DayOfWeek.of(this.value)
}