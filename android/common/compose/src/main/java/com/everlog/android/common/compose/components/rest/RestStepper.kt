package com.everlog.android.common.compose.components.rest

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.everlog.android.common.compose.elements.AppStepper
import com.everlog.android.common.kotlin.format.formatClock
import com.everlog.android.common.design.R as DesignR

// The rest after each set, with − and +, e.g. in a dialog
@Composable
fun RestStepper(seconds: Int, onStep: (Int) -> Unit) {
    AppStepper(
        value = formatClock(seconds.toLong()),
        onDecrease = { onStep(-1) },
        onIncrease = { onStep(1) },
        decreaseDescription = stringResource(DesignR.string.workout_log_rest_less),
        increaseDescription = stringResource(DesignR.string.workout_log_rest_more),
    )
}
