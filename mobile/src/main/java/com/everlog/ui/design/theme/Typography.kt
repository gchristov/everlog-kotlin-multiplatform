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
    val subtitle: TextStyle,
    val body: TextStyle,
    val bodyBold: TextStyle,
    val caption: TextStyle,
    val small: TextStyle,
    val button: TextStyle,
)

internal val LocalTypography = staticCompositionLocalOf {
    Typography(
        title = TextStyle.Default,
        heading = TextStyle.Default,
        subtitle = TextStyle.Default,
        body = TextStyle.Default,
        bodyBold = TextStyle.Default,
        caption = TextStyle.Default,
        small = TextStyle.Default,
        button = TextStyle.Default,
    )
}

internal fun typography() = Typography(
    title = Title,
    heading = Heading,
    subtitle = Subtitle,
    body = Body,
    bodyBold = BodyBold,
    caption = Caption,
    small = Small,
    button = Button,
)

// Screen titles (Toolbar.HomeTitle)
private val Title = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Medium)

// List and card row titles (row_exercise, row_routine_picker)
private val Heading = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium)

// Section headers (SettingsCategory)
private val Subtitle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)

// Row text (SettingsTitle)
private val Body = TextStyle(fontSize = 16.sp)
private val BodyBold = Body.copy(fontWeight = FontWeight.Bold)

// Row subtitles (SettingsSubtitle)
private val Caption = TextStyle(fontSize = 13.sp)

// Small labels (app version, statistics)
private val Small = TextStyle(fontSize = 12.sp)

// Button (AppCompat's default button text appearance)
private val Button = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
