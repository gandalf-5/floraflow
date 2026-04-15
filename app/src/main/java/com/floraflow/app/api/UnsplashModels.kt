package com.floraflow.app.api

import com.google.gson.annotations.SerializedName

data class UnsplashPhoto(
    @SerializedName("id") val id: String,
    @SerializedName("description") val description: String?,
    @SerializedName("alt_description") val altDescription: String?,
    @SerializedName("urls") val urls: PhotoUrls,
    @SerializedName("user") val user: UnsplashUser,
    @SerializedName("location") val location: PhotoLocation?,
    @SerializedName("links") val links: PhotoLinks
)

data class PhotoUrls(
    @SerializedName("raw") val raw: String,
    @SerializedName("full") val full: String,
    @SerializedName("regular") val regular: String,
    @SerializedName("small") val small: String
)

data class UnsplashUser(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("username") val username: String,
    @SerializedName("links") val links: UserLinks
)

data class UserLinks(
    @SerializedName("html") val html: String
)

data class PhotoLocation(
    @SerializedName("name") val name: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("country") val country: String?
)

data class PhotoLinks(
    @SerializedName("download_location") val downloadLocation: String
)

data class UnsplashSearchResult(
    @SerializedName("results") val results: List<UnsplashPhoto>,
    @SerializedName("total") val total: Int,
    @SerializedName("total_pages") val totalPages: Int
)
