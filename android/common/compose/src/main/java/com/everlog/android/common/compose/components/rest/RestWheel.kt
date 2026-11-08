package com.everlog.android.common.compose.components.rest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.everlog.android.common.compose.elements.AppWheel
import com.everlog.android.common.compose.elements.AppWheelColumn
import com.everlog.android.common.design.R as DesignR

/**
 * The rest after each set on minutes and seconds wheels, e.g. in a sheet: up to [RestWheelLimits.MaxMinutes] minutes,
 * seconds in [stepSeconds] steps. [onPick] gets the total once a wheel settles; under one step becomes one step.
 */
@Composable
fun RestWheel(seconds: Int, stepSeconds: Int, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val minutes = (0..RestWheelLimits.MaxMinutes).toList()
    val secondSteps = (0 until 60 step stepSeconds).toList()
    val (minute, second) = restWheelParts(seconds, stepSeconds)
    AppWheel(
        modifier = modifier,
        columns = listOf(
            AppWheelColumn(
                items = minutes.map { stringResource(DesignR.string.rest_wheel_minute_value, it) },
                selectedIndex = minutes.indexOf(minute),
                onPick = { onPick(restWheelTotal(minutes[it], second, stepSeconds)) },
                contentDescription = stringResource(DesignR.string.rest_wheel_minutes),
            ),
            AppWheelColumn(
                items = secondSteps.map { stringResource(DesignR.string.rest_wheel_second_value, it) },
                selectedIndex = secondSteps.indexOf(second),
                onPick = { onPick(restWheelTotal(minute, secondSteps[it], stepSeconds)) },
                contentDescription = stringResource(DesignR.string.rest_wheel_seconds),
            ),
        ),
    )
}

object RestWheelLimits {
    const val MaxMinutes = 30
}

// Minutes and seconds on the wheels' steps, rounding a saved value onto them and keeping it in range
fun restWheelParts(seconds: Int, stepSeconds: Int): Pair<Int, Int> {
    val total = restWheelTotal(0, seconds, stepSeconds)
    return total / 60 to total % 60
}

fun restWheelTotal(minutes: Int, seconds: Int, stepSeconds: Int): Int {
    val steps = Math.round((minutes * 60 + seconds) / stepSeconds.toFloat())
    return (steps * stepSeconds).coerceIn(stepSeconds, RestWheelLimits.MaxMinutes * 60 + 60 - stepSeconds)
}
