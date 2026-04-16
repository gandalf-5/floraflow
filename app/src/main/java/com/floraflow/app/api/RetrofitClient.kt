package com.floraflow.app.api

import com.floraflow.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val unsplashApi: UnsplashApi = Retrofit.Builder()
        .baseUrl(UnsplashApi.BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(UnsplashApi::class.java)

    /**
     * FloraFlow backend — proxies OpenAI calls so no API key is in the APK.
     * Base URL comes from BuildConfig.FLORA_FLOW_API_URL (set via GitHub Actions secret).
     */
    val floraFlowApi: FloraFlowApi = Retrofit.Builder()
        .baseUrl(BuildConfig.FLORA_FLOW_API_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FloraFlowApi::class.java)

    val plantNetApi: PlantNetApi = Retrofit.Builder()
        .baseUrl(PlantNetApi.BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PlantNetApi::class.java)
}
