package com.floraflow.app.api

import com.floraflow.app.BuildConfig
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the FloraFlow backend API server.
 * OpenAI calls are proxied through this server so no API keys are
 * bundled in the APK.
 */
interface FloraFlowApi {

    @POST("botanical-insight")
    suspend fun getBotanicalInsight(
        @Body request: InsightRequest
    ): InsightResponse

    @POST("botanical-quiz")
    suspend fun getBotanicalQuiz(
        @Body request: QuizRequest
    ): QuizResponse

    @POST("identify")
    suspend fun identify(
        @Body request: IdentifyRequest
    ): IdentifyApiResponse

    @POST("botanical-story")
    suspend fun getBotanicalStory(
        @Body request: StoryRequest
    ): StoryResponse

    @POST("plant-care")
    suspend fun getPlantCare(
        @Body request: CareTipsRequest
    ): CareTipsResponse

    companion object {
        /** Resolved at build time from FLORA_FLOW_API_URL env / GitHub Actions secret. */
        val BASE_URL: String get() = BuildConfig.FLORA_FLOW_API_URL
    }
}

data class InsightRequest(
    @SerializedName("plantName") val plantName: String,
    @SerializedName("nativeRegion") val nativeRegion: String? = null,
    @SerializedName("lang") val lang: String = "en"
)

data class InsightResponse(
    @SerializedName("insight") val insight: String?,
    @SerializedName("scientificName") val scientificName: String?
)

data class QuizRequest(
    @SerializedName("plantName") val plantName: String,
    @SerializedName("scientificName") val scientificName: String?,
    @SerializedName("lang") val lang: String = "en"
)

data class QuizResponse(
    @SerializedName("question") val question: String?,
    @SerializedName("options") val options: List<String>?,
    @SerializedName("correct") val correct: Int?,
    @SerializedName("explanation") val explanation: String?
)

data class IdentifyRequest(
    @SerializedName("imageBase64") val imageBase64: String,
    @SerializedName("lang") val lang: String = "en"
)

data class IdentifyApiResponse(
    @SerializedName("results") val results: List<IdentifyResult> = emptyList()
)

data class IdentifyResult(
    @SerializedName("score") val score: Double,
    @SerializedName("species") val species: IdentifySpecies
)

data class IdentifySpecies(
    @SerializedName("scientificNameWithoutAuthor") val scientificName: String,
    @SerializedName("commonNames") val commonNames: List<String> = emptyList(),
    @SerializedName("family") val family: IdentifyFamily? = null
)

data class IdentifyFamily(
    @SerializedName("scientificNameWithoutAuthor") val name: String
)

data class StoryRequest(
    @SerializedName("plantName") val plantName: String,
    @SerializedName("scientificName") val scientificName: String? = null,
    @SerializedName("lang") val lang: String = "en"
)

data class StoryResponse(
    @SerializedName("etymology") val etymology: String = "",
    @SerializedName("history") val history: String = "",
    @SerializedName("folklore") val folklore: String = "",
    @SerializedName("ecology") val ecology: String = ""
)

data class CareTipsRequest(
    @SerializedName("plantName") val plantName: String,
    @SerializedName("scientificName") val scientificName: String? = null,
    @SerializedName("lang") val lang: String = "en"
)

data class CareTipsResponse(
    @SerializedName("watering")    val watering: String = "",
    @SerializedName("light")       val light: String = "",
    @SerializedName("soil")        val soil: String = "",
    @SerializedName("temperature") val temperature: String = "",
    @SerializedName("toxicity")    val toxicity: String = "",
    @SerializedName("seasonalTip") val seasonalTip: String = ""
)
