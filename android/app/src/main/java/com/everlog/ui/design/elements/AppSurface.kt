package com.everlog.ui.design.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.theme.Theme

// A card on top of the screen background, same as the XML CardViews using background_card. Pass
// no contentPadding for content that pads itself, like an AppListItem.
@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
    contentPadding: PaddingValues = PaddingValues(Theme.spacing.large),
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        color = Theme.backgrounds.surface,
        contentColor = Theme.contentColors.primary,
        shape = Theme.shapes.surface,
        shadowElevation = elevation,
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
