package com.everlog.managers.api

import com.everlog.managers.api.coverimages.CoverImagesApi

object ApiManager {

    private var coverImagesApi = CoverImagesApi()

    fun coverImagesApi(): CoverImagesApi {
        return coverImagesApi
    }
}