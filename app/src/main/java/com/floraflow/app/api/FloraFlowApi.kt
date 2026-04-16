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

    companion object {
        /** Resolved at build time from FLORA_FLOW_API_URL env / GitHub Actions secret. */
        val BASE_URL: String get() = BuildConfig.FLORA_FLOW_API_URL
    }
}

data class InsightRequest(
    @SerializedName("plantName") val plantName: String
)

data class InsightResponse(
    @SerializedName("insight") val insight: String?,
    @SerializedName("scientificName") val scientificName: String?
)

data class QuizRequest(
    @SerializedName("plantName") val plantName: String,
    @SerializedName("scientificName") val scientificName: String?
)

data class QuizResponse(
    @SerializedName("question") val question: String?,
    @SerializedName("options") val options: List<String>?,
    @SerializedName("correct") val correct: Int?,
    @SerializedName("explanation") val explanation: String?
)
