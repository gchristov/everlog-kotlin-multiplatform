package com.everlog.android.workoutbuilder.ui.exercisetable

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.everlog.android.common.compose.elements.AppThumbnail

object GroupThumbnailSizes {
    val Size = 24.dp
    val Corner = 8.dp
}

/**
 * A superset or circuit exercise's small image, with its letter in the group (a, b, c) on its corner.
 */
@Composable
fun GroupThumbnail(imageUrl: String?, letter: Char) {
    AppThumbnail(imageUrl = imageUrl, size = GroupThumbnailSizes.Size, corner = GroupThumbnailSizes.Corner, badge = letter)
}
