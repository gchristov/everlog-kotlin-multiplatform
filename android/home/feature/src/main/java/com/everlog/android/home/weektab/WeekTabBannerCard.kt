package com.everlog.android.home.weektab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.everlog.android.common.compose.elements.AppHighlightCard
import com.everlog.android.common.compose.elements.AppImage
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.home.R

@Composable
internal fun WeekTabBannerCard(banner: WeekTabBanner, onTap: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val updateRequired = stringResource(R.string.home_banner_update_required)
    AppHighlightCard(
        modifier = modifier.testTag(WeekTabTags.Banner),
        title = banner.title,
        text = if (banner.updateRequired) "${banner.description}\n\n$updateRequired" else banner.description,
        leading = banner.imageUrl?.let { imageUrl ->
            {
                AppImage(
                    imageUrl = imageUrl,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(WeekTabSizes.BannerImage)
                        .clip(CircleShape)
                        .background(Theme.backgrounds.primary),
                )
            }
        },
        onClick = onTap,
        onClose = onClose,
        closeDescription = stringResource(R.string.home_banner_close),
        closeModifier = Modifier.testTag(WeekTabTags.BannerClose),
    )
}
