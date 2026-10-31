package com.everlog.android.plan.data.cover

import android.text.TextUtils
import com.everlog.android.common.kotlin.AppContext
import com.everlog.android.plan.data.cover.NetworkUtils
import com.everlog.android.plan.data.model.cover.CoverImagesResponse
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import rx.Observable
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.max

class CoverImagesApi {

    private val TAG = "CoverImagesApi"

    private val CACHE_TIMEOUT_ONLINE_DAYS = 1L
    private val CACHE_TIMEOUT_OFFLINE_DAYS = 7L
    private val CACHE_SIZE_MB = 10
    private val TIMEOUT_SECONDS = 60L

    private var retrofit: Retrofit? = null
    private var service: CoverImagesService? = null

    init {
        setupRetrofit()
    }

    fun getImages(query: String,
                  perPage: Int,
                  page: Int): Observable<CoverImagesResponse>? {
        return service?.getCoverImages(query, max(1, perPage), max(1, page))
    }

    // Setup

    private fun setupRetrofit() {
        retrofit = Retrofit.Builder()
                .baseUrl(CoverImagesService.API_BASE_URL)
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(setupHttpClient())
                .build()
        service = retrofit?.create(CoverImagesService::class.java)
    }

    private fun setupHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .cache(setupCache())
                .addInterceptor(setupLoggingInterceptor())
                .addInterceptor(setupRequestCacheControlInterceptor())
                .addNetworkInterceptor(setupResponseCacheControlInterceptor())
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
    }

    private fun setupCache(): Cache {
        val cacheSize = (CACHE_SIZE_MB * 1024 * 1024).toLong()
        return Cache(AppContext.cacheDir, cacheSize)
    }

    private fun setupLoggingInterceptor(): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor { message ->
            if (!TextUtils.isEmpty(message) && !message.contains("password")) {
                Timber.tag(TAG).i(message)
            }
        }
        // The request and its status are enough: each page of covers is a large body nothing needs in the logs
        interceptor.level = HttpLoggingInterceptor.Level.BASIC
        return interceptor
    }

    private fun setupRequestCacheControlInterceptor(): Interceptor {
        return Interceptor { chain ->
            var request = chain.request()
            request = if (NetworkUtils.hasNetwork(AppContext)) {
                request.newBuilder().header("Cache-Control", "public, max-age=" + TimeUnit.DAYS.toSeconds(CACHE_TIMEOUT_ONLINE_DAYS)).build()
            } else {
                request.newBuilder().header("Cache-Control", "public, only-if-cached, max-stale=" + TimeUnit.DAYS.toSeconds(CACHE_TIMEOUT_OFFLINE_DAYS)).build()
            }
            chain.proceed(request)
        }
    }

    // Based on https://elanqisthi.wordpress.com/2016/01/27/how-retrofit-with-okhttp-use-cache-data-when-offline/
    private fun setupResponseCacheControlInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, max-age=" + TimeUnit.DAYS.toSeconds(CACHE_TIMEOUT_ONLINE_DAYS))
                    .build()
        }
    }
}