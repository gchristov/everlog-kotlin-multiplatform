package com.everlog.android.plan.data.model.cover

import com.google.gson.annotations.SerializedName

class CoverImagesResponse {

    @SerializedName("total")
    var total: Int = 0
    @SerializedName("total_pages")
    var totalPages: Int = 0
    @SerializedName("results")
    var results = ArrayList<CoverImage>()
}