package com.everlog.android.home.weektab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.everlog.android.common.compose.elements.AppIconButton
import com.everlog.android.common.compose.elements.AppImage
import com.everlog.android.common.compose.elements.AppSurface
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR
import com.everlog.android.home.R

@Composable
internal fun WeekTabBannerCard(banner: WeekTabBanner, onTap: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    AppSurface(modifier = modifier.fillMaxWidth().clip(Theme.shapes.surface).clickable(onClick = onTap).testTag(WeekTabTags.Banner)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.large)) {
            banner.imageUrl?.let {
                AppImage(
                    imageUrl = it,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(WeekTabSizes.BannerImage)
                        .clip(CircleShape)
                        .background(Theme.backgrounds.primary),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall)) {
                AppText(text = banner.title, style = Theme.typography.bodyBold)
                AppText(text = banner.description, style = Theme.typography.body)
                if (banner.updateRequired) {
                    AppText(
                        text = stringResource(R.string.home_banner_update_required),
                        style = Theme.typography.small,
                        color = Theme.contentColors.secondary,
                    )
                }
            }
            AppIconButton(
                onClick = onClose,
                icon = ImageVector.vectorResource(DesignR.drawable.ic_clear_white),
                tint = Theme.contentColors.secondary,
                contentDescription = stringResource(R.string.home_banner_close),
                modifier = Modifier.align(Alignment.Top).testTag(WeekTabTags.BannerClose),
            )
        }
    }
}
