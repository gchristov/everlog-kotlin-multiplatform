package com.imagepick.picker

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class NewImagePickerResult : Parcelable {
    @Parcelize
    data class Success(val uri: Uri) : NewImagePickerResult()

    @Parcelize
    object Removed : NewImagePickerResult()

    @Parcelize
    object Cancelled : NewImagePickerResult()
}
