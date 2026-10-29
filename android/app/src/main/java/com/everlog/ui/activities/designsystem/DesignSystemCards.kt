package com.everlog.ui.activities.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppSurface
import com.everlog.ui.design.elements.AppText
import com.everlog.ui.design.theme.Theme

@Composable
internal fun DesignSystemCards(onButtonClick: () -> Unit) {
    ShowcasePage(title = "Cards") {
        group(key = "text", header = { "Card with text" }) {}
        item(key = "textExample") {
            AppSurface(modifier = Modifier.fillMaxWidth()) {
                CardText()
            }
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
