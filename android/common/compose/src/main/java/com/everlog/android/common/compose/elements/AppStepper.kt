package com.everlog.android.common.compose.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

/**
 * A value changed a step at a time, with − and + either side, e.g. in a dialog. The caller formats the value and
 * applies the steps, and turns a button off at its limit.
 */
@Composable
fun AppStepper(
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large),
    ) {
        AppIconButton(
            onClick = onDecrease,
            icon = ImageVector.vectorResource(DesignR.drawable.ic_minus),
            contentDescription = decreaseDescription,
            enabled = canDecrease,
            outlined = true,
        )
        AppText(text = value, style = Theme.typography.heading.copy(fontFeatureSettings = "tnum"))
        AppIconButton(
            onClick = onIncrease,
            icon = ImageVector.vectorResource(DesignR.drawable.ic_add),
            contentDescription = increaseDescription,
            enabled = canIncrease,
            outlined = true,
        )
    }
}

@Preview
@Composable
private fun AppStepperPreview() {
    Theme {
        AppStepper(
            value = "3",
            onDecrease = {},
            onIncrease = {},
            decreaseDescription = "Fewer",
            increaseDescription = "More",
            canIncrease = false,
        )
    }
}
