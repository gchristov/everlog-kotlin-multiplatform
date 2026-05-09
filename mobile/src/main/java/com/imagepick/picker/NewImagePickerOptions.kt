package com.imagepick.picker

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NewImagePickerOptions @JvmOverloads constructor(
    val titleResId: Int = 0,
    val allowRemove: Boolean = false,
    val aspectRatioX: Int = 1,
    val aspectRatioY: Int = 1,
    val maxWidth: Int = 512,
    val maxHeight: Int = 512,
    val lockAspectRatio: Boolean = true,
    val permissionRationaleResId: Int = 0
) : Parcelable
