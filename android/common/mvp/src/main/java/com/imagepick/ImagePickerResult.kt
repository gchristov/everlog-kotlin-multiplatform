package com.imagepick

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class ImagePickerResult : Parcelable {
    @Parcelize
    data class Success(val uri: Uri) : ImagePickerResult()

    @Parcelize
    object Removed : ImagePickerResult()

    @Parcelize
    object Cancelled : ImagePickerResult()

    @Parcelize
    object PermissionDenied : ImagePickerResult()
}
