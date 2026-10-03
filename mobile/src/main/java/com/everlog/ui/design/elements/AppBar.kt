package com.everlog.ui.design.elements

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.everlog.R
import com.everlog.ui.design.theme.Theme

// Same as the XML toolbar: black background, 20sp medium title.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(
    title: String? = null,
    showBack: Boolean = false,
    contentColor: Color = Theme.contentColors.primary,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    // Goes through the back dispatcher rather than finishing the activity, so screens can still
    // intercept back (e.g. to confirm discarding changes)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    TopAppBar(
        title = {
            title?.let { title ->
                AppText(
                    text = title,
                    color = contentColor,
                    style = Theme.typography.heading,
                    maxLines = 1,
                )
            }
        },
        navigationIcon = {
            if (showBack) {
                AppIconButton(
                    icon = ImageVector.vectorResource(R.drawable.ic_back),
                    tint = contentColor,
                    contentDescription = stringResource(R.string.app_bar_back),
                    onClick = { backDispatcher?.onBackPressed() }
                )
            }
        },
        actions = { actions?.invoke(this) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Theme.backgrounds.primary,
            scrolledContainerColor = Theme.backgrounds.primary,
        ),
    )
}
