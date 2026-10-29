package com.everlog.ui.components.exercisetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.ui.design.elements.AppIcon
import com.everlog.ui.design.theme.Theme

private object InsetDeleteSizes {
    // A selected value box's height
    val Box = 36.dp
    val Icon = 18.dp
}

/**
 * A bin drawn as an inset value box, for a delete on every row of a list, quieter than an outlined button. Being
 * smaller than 48dp, it still takes touches from the minimum touch area around it.
 */
@Composable
internal fun InsetDeleteButton(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    val shape = Theme.shapes.button
    Box(
        modifier = modifier
            .size(InsetDeleteSizes.Box)
            .clip(shape)
            .background(Theme.backgrounds.inset)
            .border(1.dp, Theme.backgrounds.separator, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            modifier = Modifier.size(InsetDeleteSizes.Icon),
            imageVector = ImageVector.vectorResource(R.drawable.ic_delete),
            tint = Theme.contentColors.destructiveSoft,
            contentDescription = contentDescription,
        )
    }
}
