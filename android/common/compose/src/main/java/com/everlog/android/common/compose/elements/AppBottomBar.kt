package com.everlog.android.common.compose.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

data class AppBottomBarItem(val label: String, val icon: ImageVector, val testTag: String = label)

/**
 * The bar of a screen's main sections along the bottom, e.g. Home's tabs: an icon over a label for each, the
 * selected one in the action colour. Same as the XML BottomNavigationView on Home, with a line above it.
 */
@Composable
fun AppBottomBar(
    items: List<AppBottomBarItem>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(Theme.backgrounds.primary)) {
        HorizontalDivider(thickness = 1.dp, color = Theme.backgrounds.separator)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(BottomBarSizes.Height)
                .selectableGroup(),
        ) {
            items.forEachIndexed { index, item ->
                val color = if (index == selected) Theme.contentColors.action else Theme.contentColors.secondary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(selected = index == selected, role = Role.Tab, onClick = { onSelected(index) })
                        .padding(vertical = Theme.spacing.small)
                        .testTag(item.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall, Alignment.CenterVertically),
                ) {
                    AppIcon(modifier = Modifier.size(BottomBarSizes.Icon), imageVector = item.icon, tint = color)
                    AppText(text = item.label, style = Theme.typography.small, color = color, maxLines = 1)
                }
            }
        }
    }
}

private object BottomBarSizes {
    val Height = 64.dp
    val Icon = 24.dp
}

@Preview
@Composable
private fun AppBottomBarPreview() {
    Theme {
        AppBottomBar(
            items = listOf(
                AppBottomBarItem("Week", ImageVector.vectorResource(DesignR.drawable.ic_home)),
                AppBottomBarItem("Workouts", ImageVector.vectorResource(DesignR.drawable.ic_barbell)),
                AppBottomBarItem("Activity", ImageVector.vectorResource(DesignR.drawable.ic_insights)),
                AppBottomBarItem("Settings", ImageVector.vectorResource(DesignR.drawable.ic_settings)),
            ),
            selected = 1,
            onSelected = {},
        )
    }
}
