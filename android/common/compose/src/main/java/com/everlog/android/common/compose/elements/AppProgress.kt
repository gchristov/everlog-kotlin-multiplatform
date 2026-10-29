package com.everlog.android.common.compose.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.everlog.android.common.compose.theme.Theme

@Composable
fun AppCircularProgressIndicator(modifier: Modifier = Modifier) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier,
        color = Theme.contentColors.action,
    )
}
