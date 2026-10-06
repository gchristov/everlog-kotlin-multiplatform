package com.everlog.ui.design.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.everlog.ui.design.theme.Theme

@Composable
fun AppCircularProgressIndicator(modifier: Modifier = Modifier) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier,
        color = Theme.contentColors.action,
    )
}

/**
 * Covers the whole screen while something loads, taking every touch so nothing underneath can be
 * used. Put it last in the screen so it draws on top.
 */
@Composable
fun AppLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.backgrounds.primary)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        AppCircularProgressIndicator()
    }
}
