package com.imagepick

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ImagePickerOptions @JvmOverloads constructor(
    val titleResId: Int = 0,
    val allowRemove: Boolean = false,
    val aspectRatioX: Int = 1,
    val aspectRatioY: Int = 1,
    val maxWidth: Int = 512,
    val maxHeight: Int = 512,
    val lockAspectRatio: Boolean = true,
    val permissionRationaleResId: Int = 0
) : Parcelable
