package com.everlog.ui.design.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.ui.design.elements.list.AppListItem
import com.everlog.ui.design.theme.Theme

// One option in an [AppMenu]
data class AppMenuItem(
    val text: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
)

/**
 * The ⋮ button with a menu of further options, usually in an [AppBar]'s actions. The menu opens
 * below the button and closes when an option is picked.
 */
@Composable
fun AppOverflowMenu(
    items: List<AppMenuItem>,
    modifier: Modifier = Modifier,
    tint: Color = Theme.contentColors.primary,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        AppIconButton(
            onClick = { expanded = true },
            icon = ImageVector.vectorResource(R.drawable.ic_more),
            tint = tint,
            contentDescription = stringResource(R.string.more_options),
        )
        AppMenu(expanded = expanded, onDismissRequest = { expanded = false }, items = items)
    }
}

/**
 * A menu of options that opens over the screen, anchored to whatever it's placed next to in a
 * [Box]: a card with the rows of a list. [AppOverflowMenu] is the usual way to open one. XML
 * screens' popup menus look the same through AppTheme.PopupOverlay (styles.xml): change both
 * together.
 */
@Composable
fun AppMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<AppMenuItem>,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.widthIn(min = MenuSizes.MinWidth),
        // In from the button's end, so in an app bar it keeps the screen's 16dp margin
        offset = DpOffset(x = -MenuSizes.EndInset, y = MenuSizes.Gap),
        shape = Theme.shapes.surface,
        containerColor = Theme.backgrounds.surface,
        tonalElevation = 0.dp,
        shadowElevation = MenuSizes.Elevation,
        border = BorderStroke(MenuSizes.Border, Theme.backgrounds.separator),
    ) {
        items.forEach { item ->
            AppListItem(
                title = item.text,
                leading = item.icon?.let { icon -> @Composable { AppIcon(imageVector = icon) } },
                onClick = {
                    onDismissRequest()
                    item.onClick()
                },
            )
        }
    }
}

private object MenuSizes {
    val MinWidth = 200.dp
    // Between the button and the menu
    val Gap = 4.dp
    val EndInset = 12.dp
    val Elevation = 8.dp
    val Border = 1.dp
}

@Preview
@Composable
private fun AppMenuPreview() {
    Theme {
        AppMenu(
            expanded = true,
            onDismissRequest = {},
            items = listOf(
                AppMenuItem(text = "Create an exercise", onClick = {}, icon = ImageVector.vectorResource(R.drawable.ic_add)),
                AppMenuItem(text = "Send feedback", onClick = {}),
            ),
        )
    }
}
