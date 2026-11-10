package com.everlog.android.common.compose.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

// Marks what the app suggests, e.g. a weight in the workout's table or the template to do next
const val AppSuggestedMark = "✦"

data class AppHighlightAction(val text: String, val onClick: () -> Unit)

/**
 * A card that stands out from the screen's other cards, for one thing the app wants the user to see: a suggestion,
 * the next step, a notification. Tinted in the action colour, like the workout's suggestion note.
 *
 * @param label A short line above the title saying what kind of thing it is, e.g. "Suggested next".
 * @param suggested Puts [AppSuggestedMark] before the label, for something the app worked out for the user.
 * @param leading Before the text, e.g. a notification's image.
 * @param trailing After the title, e.g. a few exercise images.
 * @param primary The filled button, with [secondary] outlined next to it.
 * @param onClick Makes the whole card tappable, for a card without buttons.
 * @param onClose Shows a close button in the corner.
 */
@Composable
fun AppHighlightCard(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    suggested: Boolean = false,
    text: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    primary: AppHighlightAction? = null,
    secondary: AppHighlightAction? = null,
    onClick: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    closeDescription: String? = null,
    closeModifier: Modifier = Modifier,
) {
    androidx.compose.material3.Surface(
        modifier = modifier.fillMaxWidth(),
        color = Theme.backgrounds.surface,
        contentColor = Theme.contentColors.primary,
        shape = Theme.shapes.surface,
        border = BorderStroke(1.dp, Theme.backgrounds.actionMuted),
    ) {
        Column(
            modifier = Modifier
                .background(Theme.backgrounds.selected)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(
                    start = Theme.spacing.large,
                    end = if (onClose != null) Theme.spacing.extraSmall else Theme.spacing.large,
                    top = if (onClose != null) Theme.spacing.extraSmall else Theme.spacing.large,
                    bottom = Theme.spacing.large,
                ),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                leading?.let {
                    Column(modifier = Modifier.padding(top = if (onClose != null) Theme.spacing.medium else 0.dp)) { it() }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = if (onClose != null) Theme.spacing.medium else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
                ) {
                    label?.let {
                        AppText(
                            text = if (suggested) "$AppSuggestedMark $it" else it,
                            style = Theme.typography.subtitle,
                            color = Theme.contentColors.action,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
                    ) {
                        AppText(modifier = Modifier.weight(1f), text = title, style = Theme.typography.heading)
                        trailing?.invoke()
                    }
                    text?.let { AppText(text = it, style = Theme.typography.body, color = Theme.contentColors.secondary) }
                }
                onClose?.let {
                    AppIconButton(
                        modifier = closeModifier,
                        onClick = it,
                        icon = ImageVector.vectorResource(DesignR.drawable.ic_clear_white),
                        tint = Theme.contentColors.secondary,
                        contentDescription = closeDescription,
                    )
                }
            }
            if (primary != null || secondary != null) {
                Row(
                    modifier = Modifier.padding(end = if (onClose != null) Theme.spacing.medium else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small),
                ) {
                    secondary?.let { AppSecondaryButton(modifier = Modifier.weight(1f), onClick = it.onClick, text = it.text) }
                    primary?.let { AppButton(modifier = Modifier.weight(1f), onClick = it.onClick, text = it.text) }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppHighlightCardPreview() {
    Theme {
        Column(
            modifier = Modifier.background(Theme.backgrounds.primary).padding(Theme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.large),
        ) {
            AppHighlightCard(
                label = "Suggested next",
                suggested = true,
                title = "Legs",
                text = "You've done Legs after Pull day 5 of the last 6 times.",
                primary = AppHighlightAction("Start") {},
                secondary = AppHighlightAction("Details") {},
            )
            AppHighlightCard(
                title = "Rest after the last set",
                text = "You can now rest after an exercise's last set.",
                leading = { AppThumbnail(imageUrl = null, corner = 24.dp) },
                onClick = {},
                onClose = {},
            )
        }
    }
}
