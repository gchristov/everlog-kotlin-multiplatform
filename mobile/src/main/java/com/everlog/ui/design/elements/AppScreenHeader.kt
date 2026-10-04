package com.everlog.ui.design.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.ui.design.theme.Theme

// A large title with a line of body text under it, at the top of a screen's content (e.g. "Here's your
// week"). Fixed height with the text at the bottom, so what follows always starts at the same place.
@Composable
fun AppScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(ScreenHeaderHeight),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
    ) {
        AppText(
            text = title,
            style = Theme.typography.title,
        )
        body?.let {
            AppText(
                text = it,
                style = Theme.typography.body,
                color = Theme.contentColors.secondary,
            )
        }
    }
}

private val ScreenHeaderHeight = 160.dp

@Preview
@Composable
private fun AppScreenHeaderPreview() {
    Theme {
        AppScreenHeader(
            title = "Here's your week",
            body = "4 days · Upper / Lower · Gym · Build muscle",
        )
    }
}
