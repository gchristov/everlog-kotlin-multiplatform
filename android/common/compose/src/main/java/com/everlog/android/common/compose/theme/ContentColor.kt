package com.everlog.android.common.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.everlog.android.common.design.R as DesignR

@Immutable
data class ContentColors(
    val action: Color,
    // The action colour while it's pressed, e.g. a selectable card's outline
    val actionPressed: Color,
    val onAction: Color,
    val primary: Color,
    val secondary: Color,
    val destructive: Color,
    // A quieter destructive, for a delete repeated on every row, e.g. a set's bin
    val destructiveSoft: Color,
    // Something to keep an eye on, e.g. a rest counting down
    val warning: Color,
)

internal val LocalContentColors = staticCompositionLocalOf {
    ContentColors(
        action = Color.Unspecified,
        actionPressed = Color.Unspecified,
        onAction = Color.Unspecified,
        primary = Color.Unspecified,
        secondary = Color.Unspecified,
        destructive = Color.Unspecified,
        destructiveSoft = Color.Unspecified,
        warning = Color.Unspecified,
    )
}

@Composable
internal fun contentColors() = ContentColors(
    action = colorResource(DesignR.color.main_accent),
    actionPressed = colorResource(DesignR.color.main_accent_darker),
    onAction = colorResource(DesignR.color.background_card),
    primary = colorResource(DesignR.color.white_darker),
    secondary = colorResource(DesignR.color.gray_1),
    destructive = colorResource(DesignR.color.remove),
    destructiveSoft = colorResource(DesignR.color.remove_soft),
    warning = colorResource(DesignR.color.warning),
)
