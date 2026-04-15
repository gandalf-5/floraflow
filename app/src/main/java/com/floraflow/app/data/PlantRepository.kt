package com.floraflow.app.data

import android.util.Log
import com.floraflow.app.api.ChatCompletionRequest
import com.floraflow.app.api.ChatMessage
import com.floraflow.app.api.OpenAiApi
import com.floraflow.app.api.UnsplashApi
import com.floraflow.app.api.UnsplashPhoto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlantRepository(
    private val dao: DailyPlantDao,
    private val unsplashApi: UnsplashApi,
    private val openAiApi: OpenAiApi
) {
    companion object {
        private const val TAG = "PlantRepository"

        private val PLANT_QUERIES = listOf(
            "tropical plant", "wildflower meadow", "fern forest",
            "succulent garden", "orchid", "bonsai tree", "botanical garden",
            "moss forest", "water lily", "cactus desert", "cherry blossom",
            "lavender field", "sunflower", "magnolia tree", "lotus flower"
        )

        private val PLANT_NAMES = mapOf(
            "tropical" to "Tropical Foliage",
            "wildflower" to "Wild Meadow Flowers",
            "fern" to "Forest Fern",
            "succulent" to "Desert Succulent",
            "orchid" to "Exotic Orchid",
            "bonsai" to "Ancient Bonsai",
            "botanical" to "Botanical Specimen",
            "moss" to "Old-Growth Moss",
            "water lily" to "Aquatic Lily",
            "cactus" to "Desert Cactus",
            "cherry blossom" to "Cherry Blossom",
            "lavender" to "Lavender",
            "sunflower" to "Sunflower",
            "magnolia" to "Magnolia",
            "lotus" to "Sacred Lotus"
        )
    }

    fun getTodayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    suspend fun getTodayPlant(): DailyPlant? {
        return dao.getByDate(getTodayKey())
    }

    suspend fun fetchAndSaveTodayPlant(): DailyPlant {
        val dateKey = getTodayKey()
        val existing = dao.getByDate(dateKey)
        if (existing != null) return existing

        val dayOfYear = SimpleDateFormat("D", Locale.US).format(Date()).toInt()
        val query = PLANT_QUERIES[dayOfYear % PLANT_QUERIES.size]

        val photo = try {
            unsplashApi.getRandomPhoto(query = query)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch from Unsplash", e)
            throw e
        }

        val plantName = inferPlantName(photo, query)
        val location = buildLocationString(photo)
        val insight = fetchBotanicalInsight(plantName)

        val plant = DailyPlant(
            dateKey = dateKey,
            photoId = photo.id,
            imageUrlFull = photo.urls.full,
            imageUrlRegular = photo.urls.regular,
            plantName = plantName,
            locationName = location,
            photographerName = photo.user.name,
            photographerUsername = photo.user.username,
            photographerProfileUrl = photo.user.links.html,
            downloadLocationUrl = photo.links.downloadLocation,
            botanicalInsight = insight
        )

        dao.insert(plant)
        return plant
    }

    private fun inferPlantName(photo: UnsplashPhoto, query: String): String {
        val description = (photo.description ?: photo.altDescription ?: "").lowercase()
        for ((keyword, name) in PLANT_NAMES) {
            if (description.contains(keyword) || query.contains(keyword)) {
                return name
            }
        }
        val desc = photo.description ?: photo.altDescription
        if (!desc.isNullOrBlank()) {
            return desc.replaceFirstChar { it.titlecase() }.take(40)
        }
        return query.replaceFirstChar { it.titlecase() }
    }

    private fun buildLocationString(photo: UnsplashPhoto): String? {
        val loc = photo.location ?: return null
        return listOfNotNull(loc.name, loc.city, loc.country)
            .filter { it.isNotBlank() }
            .firstOrNull()
    }

    private suspend fun fetchBotanicalInsight(plantName: String): String {
        return try {
            val prompt = "Give one fascinating, lesser-known botanical fact about $plantName in exactly 2-3 sentences. Be specific and surprising, not generic. No markdown."
            val response = openAiApi.getChatCompletion(
                authorization = "Bearer ${getOpenAiKey()}",
                request = ChatCompletionRequest(
                    messages = listOf(
                        ChatMessage(role = "system", content = "You are a knowledgeable botanist who loves sharing surprising plant facts."),
                        ChatMessage(role = "user", content = prompt)
                    )
                )
            )
            response.choices.firstOrNull()?.message?.content?.trim()
                ?: getFallbackInsight(plantName)
        } catch (e: Exception) {
            Log.w(TAG, "AI insight unavailable, using fallback", e)
            getFallbackInsight(plantName)
        }
    }

    private fun getOpenAiKey(): String {
        return try {
            val clazz = Class.forName("com.floraflow.app.BuildConfig")
            clazz.getField("OPENAI_API_KEY").get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getFallbackInsight(plantName: String): String {
        val insights = listOf(
            "Plants communicate through an underground network of fungi called the 'Wood Wide Web', sharing nutrients and chemical signals with neighboring plants.",
            "Many flowering plants time their blooms to coincide with the peak activity periods of their specific pollinators, a relationship refined over millions of years.",
            "Some plants can detect the sounds of caterpillars chewing and release defensive chemicals before any damage occurs — a remarkable form of acoustic sensing.",
            "The oldest living plant on Earth is a Posidonia australis seagrass meadow in Australia, estimated to be around 4,500 years old and stretching 180 km.",
            "Flowers appear to have UV patterns invisible to humans that act as landing guides for bees, essentially functioning as natural runways for pollinators."
        )
        return insights[(plantName.hashCode() and Int.MAX_VALUE) % insights.size]
    }

    suspend fun triggerDownload(downloadUrl: String) {
        try {
            unsplashApi.triggerDownload(downloadUrl)
        } catch (e: Exception) {
            Log.w(TAG, "Download trigger failed", e)
        }
    }
}
