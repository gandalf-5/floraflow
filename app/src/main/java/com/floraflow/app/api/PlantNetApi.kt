package com.floraflow.app.api

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface PlantNetApi {

    @Multipart
    @POST("identify/all")
    suspend fun identify(
        @Query("api-key") apiKey: String,
        @Query("lang") lang: String = "en",
        @Part image: MultipartBody.Part
    ): PlantNetResponse

    companion object {
        const val BASE_URL = "https://my-api.plantnet.org/v2/"
        const val API_KEY = "2b10oiLgd0yalCBVTL5Rrq1Ee"
    }
}

data class PlantNetResponse(
    @SerializedName("results") val results: List<PlantNetResult> = emptyList(),
    @SerializedName("query") val query: PlantNetQuery? = null
)

data class PlantNetResult(
    @SerializedName("score") val score: Double,
    @SerializedName("species") val species: PlantNetSpecies
)

data class PlantNetSpecies(
    @SerializedName("scientificNameWithoutAuthor") val scientificName: String,
    @SerializedName("commonNames") val commonNames: List<String> = emptyList(),
    @SerializedName("family") val family: PlantNetFamily? = null
)

data class PlantNetFamily(
    @SerializedName("scientificNameWithoutAuthor") val name: String
)

data class PlantNetQuery(
    @SerializedName("project") val project: String? = null
)
