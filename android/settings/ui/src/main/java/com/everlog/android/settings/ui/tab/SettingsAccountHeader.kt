package com.everlog.android.settings.ui.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.AppIcon
import com.everlog.android.common.compose.elements.AppImage
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.theme.Theme
import com.everlog.android.common.design.R as DesignR

// The signed-in user's photo, or a person icon without one, over their first name
@Composable
internal fun SettingsAccountHeader(account: SettingsAccountInfo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium),
    ) {
        Box(
            modifier = Modifier
                .size(AccountHeaderSizes.Avatar)
                .clip(CircleShape)
                .background(Theme.backgrounds.surface),
            contentAlignment = Alignment.Center,
        ) {
            if (account.photoUrl != null) {
                AppImage(
                    modifier = Modifier.size(AccountHeaderSizes.Avatar),
                    imageUrl = account.photoUrl,
                    contentScale = ContentScale.Crop,
                )
            } else {
                AppIcon(
                    modifier = Modifier.size(AccountHeaderSizes.Icon),
                    imageVector = ImageVector.vectorResource(DesignR.drawable.ic_user),
                )
            }
        }
        account.name?.let { AppText(text = it, style = Theme.typography.heading) }
    }
}

private object AccountHeaderSizes {
    val Avatar = 88.dp
    val Icon = 36.dp
}

@Preview
@Composable
private fun SettingsAccountHeaderPreview() {
    Theme {
        SettingsAccountHeader(SettingsAccountInfo(name = "Alex", photoUrl = null))
    }
}
