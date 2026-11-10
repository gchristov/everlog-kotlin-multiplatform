package com.everlog.android.common.compose.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

// An image from a URL, loaded and cached by Coil
@Composable
fun AppImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    AsyncImage(
        modifier = modifier,
        model = imageUrl,
        contentDescription = contentDescription,
        contentScale = contentScale,
    )
}

/**
 * A row's small square image, e.g. an exercise's illustration as an [com.everlog.android.common.compose.elements.list.AppListItem]'s
 * leading content. The illustrations are drawn on white, so it has a white background. Without an
 * image it shows [placeholder] on a raised card instead. A smaller [size] needs a smaller [corner],
 * as the default one would round it into a circle.
 *
 * @param badge On its corner, e.g. an exercise's letter in a superset (a, b, c).
 */
@Composable
fun AppThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = ThumbnailSizes.Size,
    corner: Dp = ThumbnailSizes.Corner,
    contentDescription: String? = null,
    placeholder: ImageVector = ImageVector.vectorResource(DesignR.drawable.ic_barbell),
    badge: Char? = null,
) {
    if (badge == null) {
        ThumbnailImage(imageUrl, modifier, size, corner, contentDescription, placeholder)
        return
    }
    Box(modifier = modifier) {
        ThumbnailImage(imageUrl, Modifier, size, corner, contentDescription, placeholder)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = ThumbnailSizes.BadgeOffset, y = ThumbnailSizes.BadgeOffset)
                .size(ThumbnailSizes.Badge)
                .background(Theme.backgrounds.primary, CircleShape)
                .border(1.dp, Theme.backgrounds.separator, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppText(text = badge.toString(), style = Theme.typography.small, color = Theme.contentColors.primary)
        }
    }
}

@Composable
private fun ThumbnailImage(
    imageUrl: String?,
    modifier: Modifier,
    size: Dp,
    corner: Dp,
    contentDescription: String?,
    placeholder: ImageVector,
) {
    val shape = RoundedCornerShape(corner)
    if (imageUrl != null) {
        AppImage(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(Color.White),
            imageUrl = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(Theme.backgrounds.surfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(imageVector = placeholder, contentDescription = contentDescription, tint = Theme.contentColors.secondary)
        }
    }
}

private object ThumbnailSizes {
    val Size = 48.dp
    val Corner = 12.dp
    val Badge = 16.dp
    val BadgeOffset = 3.dp
}

@Preview
@Composable
private fun AppThumbnailPreview() {
    Theme {
        AppThumbnail(imageUrl = null)
    }
}
