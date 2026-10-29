package com.everlog.managers.api.coverimages.response

import android.graphics.Color
import com.google.gson.annotations.SerializedName

class CoverImage {

    @SerializedName("width")
    var width: Int? = 0
    @SerializedName("height")
    var height: Int? = 0
    @SerializedName("color")
    var color: String? = null
    @SerializedName("urls")
    var urls: Map<String, String>? = null

    fun getUrl(): String? {
        var image = urls?.get("regular")
        if (image == null) {
            image = urls?.get("full")
        }
        if (image == null) {
            image = urls?.get("raw")
        }
        return image
    }

    fun getAspectRatio(): Float {
        var aspect = 1f
        if (width != null && height != null) {
            if (width != 0 && height != 0) {
                aspect = width!!.toFloat() / height!!
            }
        }
        return aspect
    }

    fun getBackgroundColor(): Int {
        return Color.parseColor(color ?: "#808080")
    }
}