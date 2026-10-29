package com.everlog.ui.design.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.theme.Theme
import com.everlog.android.common.design.R as DesignR

/**
 * Whether a row is picked, at the end of the row: an empty ring, or filled in the accent with a tick.
 * It only shows the state; the row handles the tap.
 */
@Composable
fun AppCheck(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(CheckSizes.Size)
            .clip(CircleShape)
            .border(CheckSizes.Ring, if (checked) Theme.contentColors.action else Theme.backgrounds.separator, CircleShape)
            .background(if (checked) Theme.contentColors.action else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            AppIcon(
                modifier = Modifier.size(CheckSizes.Tick),
                imageVector = ImageVector.vectorResource(DesignR.drawable.ic_check),
                tint = Theme.contentColors.onAction,
            )
        }
    }
}

private object CheckSizes {
    val Size = 24.dp
    val Ring = 2.dp
    val Tick = 14.dp
}

@Preview
@Composable
private fun AppCheckPreview() {
    Theme {
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            AppCheck(checked = false)
            AppCheck(checked = true)
        }
    }
}
