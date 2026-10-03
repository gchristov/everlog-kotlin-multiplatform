package com.everlog.ui.design.elements

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.everlog.ui.design.theme.Theme

@Composable
fun AppScreen(
    topBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = topBar,
        containerColor = Theme.backgrounds.primary,
        contentColor = Theme.contentColors.primary,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // So children using e.g. imePadding() don't add the system bars again
                .consumeWindowInsets(padding)
        ) {
            content()
        }
    }
}
