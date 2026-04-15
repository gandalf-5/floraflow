package com.floraflow.app.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface UnsplashApi {

    @GET("photos/random")
    suspend fun getRandomPhoto(
        @Query("query") query: String = "botanical plant flower",
        @Query("orientation") orientation: String = "portrait",
        @Query("client_id") clientId: String = UNSPLASH_ACCESS_KEY
    ): UnsplashPhoto

    @GET("search/photos")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30,
        @Query("orientation") orientation: String = "portrait",
        @Query("client_id") clientId: String = UNSPLASH_ACCESS_KEY
    ): UnsplashSearchResult

    @GET
    suspend fun triggerDownload(
        @Url url: String,
        @Query("client_id") clientId: String = UNSPLASH_ACCESS_KEY
    ): Any

    companion object {
        const val UNSPLASH_ACCESS_KEY = "q6e0o9Gy6boMIbmXe-YXP44JinlKUZr67D6H-plaSoA"
        const val BASE_URL = "https://api.unsplash.com/"
    }
}
