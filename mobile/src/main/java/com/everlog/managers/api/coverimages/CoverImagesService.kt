package com.everlog.managers.api.coverimages

import com.everlog.managers.api.coverimages.response.CoverImagesResponse
import retrofit2.http.GET
import retrofit2.http.Query
import rx.Observable

interface CoverImagesService {

    companion object {

        const val API_BASE_URL = "https://api.unsplash.com/"
        const val API_CLIENT_ID = "1ea22389578d6197b41c6b7aaac0ef4a1077ea9f2f1935a059f7f3af46af339a"
    }

    @GET("search/photos")
    fun getCoverImages(@Query("query") query: String,
                       @Query("per_page") perPage: Int,
                       @Query("page") page: Int,
                       @Query("client_id") clientId: String = API_CLIENT_ID,
                       @Query("orientation") orientation: String = "landscape"): Observable<CoverImagesResponse>
}