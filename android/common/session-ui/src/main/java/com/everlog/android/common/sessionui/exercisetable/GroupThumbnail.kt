package com.everlog.android.common.sessionui.exercisetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.AppText
import com.everlog.android.common.compose.elements.AppThumbnail
import com.everlog.android.common.compose.theme.Theme

object GroupThumbnailSizes {
    val Size = 24.dp
    val Corner = 8.dp
    val Badge = 16.dp
    val BadgeOffset = 3.dp
}

/**
 * A superset or circuit exercise's small image, with its letter in the group (a, b, c) on its corner.
 */
@Composable
fun GroupThumbnail(imageUrl: String?, letter: Char) {
    Box {
        AppThumbnail(imageUrl = imageUrl, size = GroupThumbnailSizes.Size, corner = GroupThumbnailSizes.Corner)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = GroupThumbnailSizes.BadgeOffset, y = GroupThumbnailSizes.BadgeOffset)
                .size(GroupThumbnailSizes.Badge)
                .background(Theme.backgrounds.primary, CircleShape)
                .border(1.dp, Theme.backgrounds.separator, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppText(text = letter.toString(), style = Theme.typography.small, color = Theme.contentColors.primary)
        }
    }
}
