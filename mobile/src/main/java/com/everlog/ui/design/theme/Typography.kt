package com.everlog.ui.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Sizes and weights follow the existing XML styles (styles.xml, dimens.xml), where "sans-serif-medium"
// is FontWeight.Medium.
@Immutable
data class Typography(
    val title: TextStyle,
    val heading: TextStyle,
    val subheading: TextStyle,
    val body: TextStyle,
    val bodyBold: TextStyle,
    val caption: TextStyle,
    val button: TextStyle,
)

internal val LocalTypography = staticCompositionLocalOf {
    Typography(
        title = TextStyle.Default,
        heading = TextStyle.Default,
        subheading = TextStyle.Default,
        body = TextStyle.Default,
        bodyBold = TextStyle.Default,
        caption = TextStyle.Default,
        button = TextStyle.Default,
    )
}

internal fun typography() = Typography(
    title = Title,
    heading = Heading,
    subheading = Subheading,
    body = Body,
    bodyBold = BodyBold,
    caption = Caption,
    button = Button,
)

// Toolbar.HomeTitle
private val Title = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Medium)

// SettingsCategory.Large
private val Heading = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium)

// SettingsCategory
private val Subheading = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)

// SettingsTitle
private val Body = TextStyle(fontSize = 16.sp)
private val BodyBold = Body.copy(fontWeight = FontWeight.Bold)

// SettingsSubtitle
private val Caption = TextStyle(fontSize = 13.sp)

// Button (AppCompat's default button text appearance)
private val Button = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
