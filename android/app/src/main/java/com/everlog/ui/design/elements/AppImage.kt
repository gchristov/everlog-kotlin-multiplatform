package com.everlog.ui.design.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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
import com.everlog.ui.design.theme.Theme
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
 * A row's small square image, e.g. an exercise's illustration as an [com.everlog.ui.design.elements.list.AppListItem]'s
 * leading content. The illustrations are drawn on white, so it has a white background. Without an
 * image it shows [placeholder] on a raised card instead. A smaller [size] needs a smaller [corner],
 * as the default one would round it into a circle.
 */
@Composable
fun AppThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = ThumbnailSizes.Size,
    corner: Dp = ThumbnailSizes.Corner,
    contentDescription: String? = null,
    placeholder: ImageVector = ImageVector.vectorResource(DesignR.drawable.ic_barbell),
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
}

@Preview
@Composable
private fun AppThumbnailPreview() {
    Theme {
        AppThumbnail(imageUrl = null)
    }
}
