package com.everlog.android.common.compose.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

/**
 * The screen's main action, floating over its content at the bottom end, e.g. Home's Start workout: a filled pill
 * with an icon and a label, like the XML one on Home. It pops in when it appears and fades out when it goes.
 */
@Composable
fun AppFloatingButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = fadeOut(),
    ) {
        androidx.compose.material3.Surface(
            onClick = onClick,
            shape = Theme.shapes.button,
            color = Theme.contentColors.action,
            contentColor = Theme.contentColors.onAction,
            shadowElevation = FloatingButtonSizes.Elevation,
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = Theme.sizes.button)
                    .padding(FloatingButtonSizes.Padding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
            ) {
                AppIcon(modifier = Modifier.size(FloatingButtonSizes.Icon), imageVector = icon, tint = Theme.contentColors.onAction)
                AppText(text = text, style = Theme.typography.button, color = Theme.contentColors.onAction)
            }
        }
    }
}

private object FloatingButtonSizes {
    val Padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    val Icon = 20.dp
    val Elevation = 6.dp
}

@Preview
@Composable
private fun AppFloatingButtonPreview() {
    Theme {
        AppFloatingButton(text = "Start workout", icon = ImageVector.vectorResource(DesignR.drawable.ic_add), onClick = {})
    }
}
