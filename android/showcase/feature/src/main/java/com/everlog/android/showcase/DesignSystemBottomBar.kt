package com.everlog.android.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.elements.AppBar
import com.everlog.android.common.compose.elements.AppBottomBar
import com.everlog.android.common.compose.elements.AppBottomBarItem
import com.everlog.android.common.compose.elements.AppFloatingButton
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

// Sections along the bottom, and the floating button each can have: on the second, a different one; none on the last
@Composable
internal fun DesignSystemBottomBar() {
    var selected by remember { mutableIntStateOf(0) }
    val items = listOf(
        AppBottomBarItem("Week", ImageVector.vectorResource(DesignR.drawable.ic_home)),
        AppBottomBarItem("Workouts", ImageVector.vectorResource(DesignR.drawable.ic_barbell)),
        AppBottomBarItem("Activity", ImageVector.vectorResource(DesignR.drawable.ic_insights)),
        AppBottomBarItem("Settings", ImageVector.vectorResource(DesignR.drawable.ic_settings)),
    )
    Column(modifier = Modifier.fillMaxSize().background(Theme.backgrounds.primary)) {
        AppBar(title = "Bottom bar", showBack = true)
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            AppText(
                modifier = Modifier.align(Alignment.Center),
                text = "${items[selected].label}: tap the bar to switch",
                color = Theme.contentColors.secondary,
            )
            AppFloatingButton(
                modifier = Modifier.align(Alignment.BottomEnd).padding(Theme.spacing.large),
                text = if (selected == 1) "New" else "Start workout",
                icon = ImageVector.vectorResource(DesignR.drawable.ic_add),
                onClick = {},
                visible = selected != items.lastIndex,
            )
        }
        AppBottomBar(items = items, selected = selected, onSelected = { selected = it })
    }
}

@Preview
@Composable
private fun DesignSystemBottomBarPreview() {
    Theme {
        DesignSystemBottomBar()
    }
}
