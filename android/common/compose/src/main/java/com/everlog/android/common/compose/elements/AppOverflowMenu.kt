package com.everlog.android.common.compose.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
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
import com.everlog.android.common.compose.elements.list.AppListItem
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

// One option in an [AppMenu]. [selected] ticks the current choice when the menu picks one of several,
// e.g. a time range.
data class AppMenuItem(
    val text: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val selected: Boolean = false,
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
    // The ⋮ in a thin circle, see AppIconButton
    outlined: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        AppIconButton(
            onClick = { expanded = true },
            icon = ImageVector.vectorResource(DesignR.drawable.ic_more),
            tint = tint,
            contentDescription = stringResource(DesignR.string.more_options),
            outlined = outlined,
        )
        AppMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            items = items,
            // In from the button's end, so in an app bar it keeps the screen's 16dp margin
            offset = DpOffset(x = -MenuSizes.EndInset, y = MenuSizes.Gap),
        )
    }
}

/**
 * A menu of options that opens over the screen, anchored to whatever it's placed next to in a
 * [Box]: a card with the rows of a list. [AppOverflowMenu] is the usual way to open one, or an
 * [AppChip] with a dropdown arrow to pick one of several options. XML screens' popup menus and
 * spinners look the same through AppTheme.PopupOverlay, AppSpinner (styles.xml) and
 * view_spinner_row_dropdown: change them together.
 */
@Composable
fun AppMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<AppMenuItem>,
    // From where it opens: below what it's next to, lined up with its start (or its end when there's
    // no room)
    offset: DpOffset = DpOffset(x = 0.dp, y = MenuSizes.Gap),
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.widthIn(min = MenuSizes.MinWidth),
        offset = offset,
        shape = Theme.shapes.surface,
        containerColor = Theme.backgrounds.surface,
        tonalElevation = 0.dp,
        shadowElevation = MenuSizes.Elevation,
        border = BorderStroke(MenuSizes.Border, Theme.backgrounds.separator),
    ) {
        // When some options have icons, the others leave the icon's space so the labels line up
        val icons = items.any { it.icon != null }
        items.forEach { item ->
            AppListItem(
                title = item.text,
                leading = when {
                    item.icon != null -> { { AppIcon(imageVector = item.icon) } }
                    icons -> { { Spacer(modifier = Modifier.size(MenuSizes.Icon)) } }
                    else -> null
                },
                trailing = if (item.selected) {
                    { AppIcon(imageVector = ImageVector.vectorResource(DesignR.drawable.ic_check), tint = Theme.contentColors.action) }
                } else {
                    null
                },
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
    // AppIcon's size
    val Icon = 24.dp
}

@Preview
@Composable
private fun AppMenuPreview() {
    Theme {
        AppMenu(
            expanded = true,
            onDismissRequest = {},
            items = listOf(
                AppMenuItem(text = "Create an exercise", onClick = {}, icon = ImageVector.vectorResource(DesignR.drawable.ic_add)),
                AppMenuItem(text = "Send feedback", onClick = {}),
            ),
        )
    }
}
