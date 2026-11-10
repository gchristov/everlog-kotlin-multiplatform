package com.everlog.android.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.android.common.compose.elements.AppButton
import com.everlog.android.common.compose.elements.AppExpandableCard
import com.everlog.android.common.compose.elements.AppExpandableCardGroup
import com.everlog.android.common.compose.elements.AppExpandableCardRow
import com.everlog.android.common.compose.elements.AppHighlightAction
import com.everlog.android.common.compose.elements.AppHighlightCard
import com.everlog.android.common.compose.elements.AppSecondaryButton
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.AppThumbnail
import com.everlog.android.common.compose.theme.Theme

@Composable
internal fun DesignSystemCards(onButtonClick: () -> Unit) {
    ShowcasePage(title = "Cards") {
        group(key = "text", header = { "Card with text" }) {}
        item(key = "textExample") {
            AppSurface(modifier = Modifier.fillMaxWidth()) {
                CardText()
            }
        }
        group(key = "expandable", header = { "Card that opens" }) {}
        item(key = "expandableExample") {
            ExpandableCards(onButtonClick)
        }
        group(key = "actions", header = { "Card with actions" }) {}
        item(key = "actionsExample") {
            AppSurface(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                    CardText()
                    ButtonRow(
                        enabled = true,
                        onButtonClick = onButtonClick,
                    )
                    AppButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onButtonClick,
                        text = "Full width",
                    )
                }
            }
        }
        group(key = "highlight", header = { "Highlight card" }) {}
        item(key = "highlightExamples") {
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
                AppHighlightCard(
                    label = "Suggested next",
                    suggested = true,
                    title = "Legs",
                    text = "Something the app worked out for the user, with why, and up to two buttons.",
                    trailing = { AppThumbnail(imageUrl = SampleImageUrl, size = Theme.sizes.leading) },
                    primary = AppHighlightAction("Start", onButtonClick),
                    secondary = AppHighlightAction("Details", onButtonClick),
                )
                AppHighlightCard(
                    title = "A notification",
                    text = "Tap anywhere on it, or close it.",
                    leading = { AppThumbnail(imageUrl = SampleImageUrl, corner = Theme.sizes.leading / 2) },
                    onClick = onButtonClick,
                    onClose = onButtonClick,
                )
                AppHighlightCard(
                    label = "Get started",
                    title = "Just a label, a title and text",
                    text = "Without the mark, for a next step that isn't a suggestion.",
                    primary = AppHighlightAction("Continue", onButtonClick),
                )
            }
        }
    }
}

@Composable
private fun ExpandableCards(onButtonClick: () -> Unit) {
    var open by remember { mutableStateOf(1) }
    val groups = listOf(
        AppExpandableCardGroup(listOf(AppExpandableCardRow("High Knees", SampleImageUrl, "3 × 30 s"))),
        AppExpandableCardGroup(listOf(AppExpandableCardRow("Bench Press", null, "3 × 8"))),
        AppExpandableCardGroup(
            label = "Superset",
            rows = listOf(AppExpandableCardRow("Overhead Press", SampleImageUrl, "3 × 10"), AppExpandableCardRow("Lateral Raise", null, "3 × 15")),
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
        listOf("Push day", "Pull day").forEachIndexed { index, title ->
            AppExpandableCard(
                title = title,
                subtitle = "Opens in place with a tap",
                groups = groups,
                open = open == index,
                onToggle = { open = if (open == index) -1 else index },
                footer = {
                    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)) {
                        AppSecondaryButton(modifier = Modifier.weight(1f), onClick = onButtonClick, text = "Details")
                        AppButton(modifier = Modifier.weight(1f), onClick = onButtonClick, text = "Start")
                    }
                },
            )
        }
    }
}

@Composable
private fun CardText() {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
        AppText(
            text = "Card title",
            style = Theme.typography.heading,
        )
        AppText(
            text = "The card background sits on top of the screen background, like the cards in Settings.",
            style = Theme.typography.body,
            color = Theme.contentColors.secondary,
        )
    }
}

@Preview
@Composable
private fun DesignSystemCardsPreview() {
    Theme {
        DesignSystemCards(onButtonClick = {})
    }
}

private const val SampleImageUrl = "https://storage.googleapis.com/everlog-9bddf.appspot.com/exercises/illustrations/high%20knees1.webp"
