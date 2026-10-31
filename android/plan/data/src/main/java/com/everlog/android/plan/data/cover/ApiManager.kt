package com.everlog.android.plan.data.cover

import com.everlog.android.plan.data.cover.CoverImagesApi

object ApiManager {

    private var coverImagesApi = CoverImagesApi()

    fun coverImagesApi(): CoverImagesApi {
        return coverImagesApi
    }
}