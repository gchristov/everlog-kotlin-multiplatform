package com.everlog.ui.components.rest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.everlog.R
import com.everlog.android.common.compose.elements.AppIconButton
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.kotlin.format.formatClock
import com.everlog.android.common.design.R as DesignR

// The rest after each set, with − and +, e.g. in a dialog
@Composable
internal fun RestStepper(seconds: Int, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
        AppIconButton(
            onClick = { onStep(-1) },
            icon = ImageVector.vectorResource(R.drawable.ic_minus),
            contentDescription = stringResource(R.string.workout_log_rest_less),
            outlined = true,
        )
        AppText(text = formatClock(seconds.toLong()), style = Theme.typography.heading.copy(fontFeatureSettings = "tnum"))
        AppIconButton(
            onClick = { onStep(1) },
            icon = ImageVector.vectorResource(DesignR.drawable.ic_add),
            contentDescription = stringResource(R.string.workout_log_rest_more),
            outlined = true,
        )
    }
}
