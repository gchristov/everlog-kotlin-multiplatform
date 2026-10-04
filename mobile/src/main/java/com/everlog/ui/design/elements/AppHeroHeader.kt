package com.everlog.ui.design.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.R
import com.everlog.ui.design.theme.Theme

/**
 * A large title with a line of body text under it, at the top of a screen's content (e.g. "Here's
 * your week").
 *
 * Without an [image] it has a fixed height with the text at the bottom, so what follows always
 * starts at the same place. It doesn't pad the text, so it lines up with the content under it.
 *
 * With an [image] the image goes edge to edge at a fixed height (as on the workout details cover)
 * and the text follows under it, padded to the screen margin. Place it full width.
 */
@Composable
fun AppHeroHeader(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    image: Painter? = null,
) {
    if (image == null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(HeroHeaderHeight),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
        ) {
            HeroHeaderText(title = title, body = body)
        }
    } else {
        Column(modifier = modifier.fillMaxWidth()) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeroHeaderImageHeight),
                painter = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.padding(
                    start = Theme.spacing.large,
                    top = Theme.spacing.large,
                    end = Theme.spacing.large,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HeroHeaderText(title = title, body = body)
            }
        }
    }
}

@Composable
private fun HeroHeaderText(title: String, body: String?) {
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

private val HeroHeaderHeight = 160.dp

// Same as the cover image on workout details
private val HeroHeaderImageHeight = 140.dp

@Preview
@Composable
private fun AppHeroHeaderPreview() {
    Theme {
        AppHeroHeader(
            title = "Here's your week",
            body = "4 days · Upper / Lower · Gym · Build muscle",
        )
    }
}

@Preview
@Composable
private fun AppHeroHeaderImagePreview() {
    Theme {
        AppHeroHeader(
            title = "Push day",
            body = "6 exercises · About 55 minutes",
            image = painterResource(R.drawable.design_system_hero_sample),
        )
    }
}
