package com.everlog.ui.design.elements.keypad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme

// Strips for AppKeypad's strips slot: a countdown, or a note

enum class AppKeypadTimerStyle {
    // Something being timed, e.g. a timed set: in the action colour
    Active,

    // A rest: in the warning colour
    Rest,
}

/**
 * A countdown in progress: its [label] over the [time] left, [less] and [more] (e.g. −10s and +10s), and [end] to
 * stop it early (e.g. Skip).
 */
@Composable
fun AppKeypadTimer(
    label: String,
    time: String,
    style: AppKeypadTimerStyle,
    less: AppKeypadAction,
    more: AppKeypadAction,
    end: AppKeypadAction,
    modifier: Modifier = Modifier,
) {
    val (color, background, border) = when (style) {
        AppKeypadTimerStyle.Active -> Triple(Theme.contentColors.action, Theme.backgrounds.selected, Theme.backgrounds.actionMuted)
        AppKeypadTimerStyle.Rest -> Triple(Theme.contentColors.warning, Theme.backgrounds.warningFaint, Theme.backgrounds.warningMuted)
    }
    val shape = Theme.shapes.button
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(KeypadSizes.StripHeight)
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(text = label, style = Theme.typography.small, color = color, maxLines = 1)
            AppText(text = time, style = keypadNumberStyle(), color = color, maxLines = 1)
        }
        listOf(less, more, end).forEach { KeypadPill(text = it.text, onClick = it.onClick) }
    }
}

/**
 * A line of text in a card, e.g. a hint, with an optional pill [action] at its end. [highlighted] puts the text in
 * the action colour, for something the app suggests.
 */
@Composable
fun AppKeypadNote(
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    action: AppKeypadAction? = null,
) {
    val shape = Theme.shapes.button
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (highlighted) KeypadColors.Inset else Theme.backgrounds.primary)
            .border(1.dp, if (highlighted) Theme.backgrounds.separator else Color.Transparent, shape)
            .padding(start = 14.dp, end = if (action != null) 6.dp else 14.dp, top = 6.dp, bottom = 6.dp)
            // As tall as a pill even without one, so a note doesn't look squashed
            .heightIn(min = KeypadSizes.Chip),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
    ) {
        AppText(
            modifier = Modifier.weight(1f),
            text = text,
            style = Theme.typography.caption,
            color = if (highlighted) Theme.contentColors.action else Theme.contentColors.secondary,
        )
        action?.let { KeypadPill(text = it.text, onClick = it.onClick) }
    }
}

@Preview
@Composable
private fun AppKeypadStripsPreview() {
    Theme {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
            AppKeypadTimer(
                label = "REST",
                time = "1:24",
                style = AppKeypadTimerStyle.Rest,
                less = AppKeypadAction("−10s") {},
                more = AppKeypadAction("+10s") {},
                end = AppKeypadAction("Skip") {},
            )
            AppKeypadNote(text = "Suggested 62.5 kg, up 2.5 kg on last time", highlighted = true, action = AppKeypadAction("Why?") {})
            AppKeypadNote(text = "Add an exercise to start")
        }
    }
}
