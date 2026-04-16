package com.floraflow.app.data

import android.util.Log
import com.floraflow.app.BuildConfig
import com.floraflow.app.api.ChatCompletionRequest
import com.floraflow.app.api.ChatMessage
import com.floraflow.app.api.OpenAiApi
import com.floraflow.app.api.UnsplashApi
import com.floraflow.app.api.UnsplashPhoto
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PlantRepository(
    private val dao: DailyPlantDao,
    private val unsplashApi: UnsplashApi,
    private val openAiApi: OpenAiApi,
    private val preferredCategories: List<String> = PreferencesManager.ALL_CATEGORIES
) {
    companion object {
        private const val TAG = "PlantRepository"

        /**
         * Each category maps to a list of (unsplash query, display name) pairs.
         * The Unsplash query uses the real species name so the photo always matches.
         */
        private val CATEGORY_PLANTS: Map<String, List<Pair<String, String>>> = mapOf(
            "wildflower" to listOf(
                "Papaver rhoeas poppy flower" to "Common Poppy",
                "Bellis perennis daisy" to "Common Daisy",
                "Centaurea cyanus cornflower" to "Cornflower",
                "Leucanthemum vulgare oxeye daisy" to "Oxeye Daisy",
                "Lotus corniculatus trefoil" to "Bird's-foot Trefoil",
                "Ranunculus acris buttercup flower" to "Meadow Buttercup",
                "Campanula rotundifolia harebell" to "Harebell",
                "Prunella vulgaris selfheal wildflower" to "Self-heal"
            ),
            "tropical" to listOf(
                "Strelitzia reginae bird of paradise flower" to "Bird of Paradise",
                "Heliconia rostrata lobster claw flower" to "Lobster Claw",
                "Plumeria frangipani tropical flower" to "Frangipani",
                "Anthurium andreanum flamingo flower" to "Flamingo Flower",
                "Hibiscus rosa-sinensis tropical" to "Chinese Hibiscus",
                "Alpinia purpurata red ginger flower" to "Red Ginger",
                "Calathea orbifolia tropical leaf" to "Calathea",
                "Monstera deliciosa tropical plant" to "Swiss Cheese Plant"
            ),
            "fern" to listOf(
                "Dryopteris filix-mas male fern forest" to "Male Fern",
                "Osmunda regalis royal fern" to "Royal Fern",
                "Asplenium nidus bird's nest fern" to "Bird's Nest Fern",
                "Polypodium vulgare common polypody" to "Common Polypody",
                "Athyrium filix-femina lady fern" to "Lady Fern",
                "Adiantum capillus-veneris maidenhair fern" to "Maidenhair Fern",
                "Cyathea tree fern" to "Tree Fern",
                "Matteuccia struthiopteris ostrich fern" to "Ostrich Fern"
            ),
            "succulent" to listOf(
                "Echeveria elegans Mexican snowball succulent" to "Mexican Snowball",
                "Aloe vera plant" to "Aloe Vera",
                "Sedum acre stonecrop succulent" to "Biting Stonecrop",
                "Sempervivum tectorum houseleek succulent" to "Common Houseleek",
                "Agave americana succulent" to "Century Plant",
                "Haworthia attenuata zebra plant succulent" to "Zebra Plant",
                "Crassula ovata jade plant" to "Jade Plant",
                "Dudleya brittonii chalk liveforever" to "Chalk Liveforever"
            ),
            "orchid" to listOf(
                "Phalaenopsis amabilis moth orchid" to "Moth Orchid",
                "Cattleya labiata corsage orchid" to "Corsage Orchid",
                "Dendrobium nobile orchid" to "Noble Dendrobium",
                "Vanda coerulea blue orchid" to "Blue Vanda",
                "Ophrys apifera bee orchid wild" to "Bee Orchid",
                "Dactylorhiza fuchsii spotted orchid" to "Common Spotted Orchid",
                "Paphiopedilum slipper orchid" to "Lady's Slipper",
                "Coelogyne cristata orchid" to "Necklace Orchid"
            ),
            "bonsai" to listOf(
                "Juniperus procumbens bonsai tree" to "Juniper Bonsai",
                "Ficus retusa bonsai tree" to "Ficus Bonsai",
                "Acer palmatum maple bonsai" to "Japanese Maple Bonsai",
                "Pinus thunbergii black pine bonsai" to "Japanese Black Pine Bonsai",
                "Prunus mume ume bonsai" to "Japanese Apricot Bonsai",
                "Zelkova serrata bonsai tree" to "Japanese Zelkova Bonsai",
                "Carmona retusa fukien tea bonsai" to "Fukien Tea Bonsai",
                "Ulmus parvifolia chinese elm bonsai" to "Chinese Elm Bonsai"
            ),
            "moss" to listOf(
                "Bryum argenteum silver moss close up" to "Silver-green Bryum",
                "Sphagnum moss bog peat" to "Peat Moss",
                "Hypnum cupressiforme cypress-leaved plait-moss" to "Cypress-leaved Moss",
                "Polytrichum commune common haircap moss" to "Common Haircap Moss",
                "Plagiomnium undulatum wavy-leaved thread-moss" to "Wavy Feather Moss",
                "Dicranum scoparium broom fork-moss" to "Broom Fork Moss",
                "Thuidium tamariscinum common tamarisk-moss" to "Tamarisk Moss",
                "Leucobryum glaucum white cushion moss" to "White Cushion Moss"
            ),
            "water lily" to listOf(
                "Nymphaea alba white water lily" to "White Water Lily",
                "Nuphar lutea yellow water lily" to "Yellow Water Lily",
                "Victoria amazonica giant water lily" to "Giant Amazon Water Lily",
                "Nelumbo nucifera sacred lotus water" to "Sacred Lotus",
                "Nymphaea 'Black Princess' water lily" to "Black Princess Lily",
                "Nymphaea caerulea blue lotus" to "Blue Egyptian Lotus",
                "Euryale ferox foxnut water plant" to "Foxnut",
                "Nymphaea 'Attraction' pink water lily" to "Attraction Water Lily"
            ),
            "cactus" to listOf(
                "Cereus jamacaru column cactus" to "Jamacaru Cactus",
                "Opuntia ficus-indica prickly pear cactus" to "Prickly Pear",
                "Echinocactus grusonii golden barrel cactus" to "Golden Barrel Cactus",
                "Ferocactus wislizeni fishhook barrel cactus" to "Fishhook Barrel",
                "Carnegiea gigantea saguaro cactus" to "Saguaro Cactus",
                "Mammillaria hahniana old lady cactus" to "Old Lady Cactus",
                "Astrophytum myriostigma bishop hat cactus" to "Bishop's Hat",
                "Notocactus ottonis ball cactus flowering" to "Otto's Cactus"
            ),
            "cherry blossom" to listOf(
                "Prunus serrulata Yoshino cherry blossom" to "Yoshino Cherry",
                "Prunus x yedoensis cherry blossom" to "Tokyo Cherry",
                "Prunus avium wild cherry blossom" to "Wild Cherry",
                "Prunus 'Kanzan' double cherry blossom" to "Kanzan Cherry",
                "Prunus subhirtella spring cherry" to "Spring Cherry",
                "Prunus 'Ukon' pale yellow cherry blossom" to "Ukon Cherry",
                "Prunus cerasifera myrobalan plum blossom" to "Cherry Plum",
                "Prunus padus bird cherry blossom" to "Bird Cherry"
            ),
            "lavender" to listOf(
                "Lavandula angustifolia true lavender field" to "True Lavender",
                "Lavandula stoechas French lavender" to "French Lavender",
                "Lavandula dentata fringed lavender" to "Fringed Lavender",
                "Lavandula x intermedia lavandin" to "Lavandin",
                "Lavandula multifida fernleaf lavender" to "Fernleaf Lavender",
                "Lavandula lanata woolly lavender" to "Woolly Lavender",
                "Lavandula latifolia spike lavender" to "Spike Lavender",
                "Lavandula canariensis canary island lavender" to "Canary Lavender"
            ),
            "sunflower" to listOf(
                "Helianthus annuus common sunflower field" to "Common Sunflower",
                "Helianthus debilis beach sunflower" to "Beach Sunflower",
                "Helianthus tuberosus Jerusalem artichoke flower" to "Jerusalem Artichoke",
                "Helianthus 'Teddy Bear' double sunflower" to "Teddy Bear Sunflower",
                "Heliopsis helianthoides false sunflower" to "Smooth Oxeye",
                "Helianthus 'Moulin Rouge' red sunflower" to "Moulin Rouge Sunflower",
                "Helianthus maximiliani Maximilian sunflower" to "Maximilian Sunflower",
                "Helianthus multiflorus perennial sunflower" to "Many-flowered Sunflower"
            ),
            "magnolia" to listOf(
                "Magnolia grandiflora southern magnolia" to "Southern Magnolia",
                "Magnolia stellata star magnolia" to "Star Magnolia",
                "Magnolia liliiflora purple magnolia" to "Mulan Magnolia",
                "Magnolia x soulangeana saucer magnolia" to "Saucer Magnolia",
                "Magnolia sieboldii oyama magnolia" to "Oyama Magnolia",
                "Magnolia campbellii pink himalayan magnolia" to "Campbell's Magnolia",
                "Magnolia obovata Japanese big leaf magnolia" to "Japanese Magnolia",
                "Magnolia denudata yulan magnolia" to "Yulan Magnolia"
            ),
            "lotus" to listOf(
                "Nelumbo nucifera pink sacred lotus" to "Sacred Lotus",
                "Nelumbo lutea American lotus yellow" to "American Lotus",
                "Nelumbo 'Mrs. Perry D. Slocum' lotus" to "Perry's Lotus",
                "Nelumbo 'Momo Botan' double lotus" to "Momo Botan Lotus",
                "Nelumbo 'Carolina Queen' lotus" to "Carolina Queen Lotus",
                "Nelumbo 'Baby Doll' miniature lotus" to "Baby Doll Lotus",
                "Nelumbo nucifera alba white lotus" to "White Sacred Lotus",
                "Nelumbo 'Chawan Basu' lotus" to "Chawan Basu Lotus"
            )
        )

        private val ALL_PLANTS: List<Pair<String, String>> =
            CATEGORY_PLANTS.values.flatten()
    }

    fun getTodayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    suspend fun getTodayPlant(): DailyPlant? = dao.getByDate(getTodayKey())

    suspend fun getPlantByKey(dateKey: String): DailyPlant? = dao.getByDate(dateKey)

    suspend fun getHistory(): List<DailyPlant> = dao.getHistory()

    suspend fun getFavorites(): List<DailyPlant> = dao.getFavorites()

    suspend fun getAllForSeasonal(): List<DailyPlant> = dao.getAllForSeasonal()

    suspend fun toggleFavorite(plant: DailyPlant) {
        dao.setFavorite(plant.dateKey, !plant.isFavorite)
    }

    suspend fun updateNotes(dateKey: String, notes: String?) {
        dao.updateNotes(dateKey, notes)
    }

    suspend fun fetchAndSaveTodayPlant(): DailyPlant {
        val dateKey = getTodayKey()
        val existing = dao.getByDate(dateKey)
        if (existing != null) return existing

        val (query, displayName) = pickPlantForToday()
        return fetchAndSave(dateKey, query, displayName)
    }

    suspend fun fetchForCategory(categoryQuery: String): DailyPlant {
        val dateKey = "${getTodayKey()}-$categoryQuery-${System.currentTimeMillis()}"
        val plants = CATEGORY_PLANTS[categoryQuery] ?: ALL_PLANTS
        val pick = plants[(System.currentTimeMillis() / 1000).toInt() % plants.size]
        return fetchAndSave(dateKey, pick.first, pick.second, forceNew = true)
    }

    private fun pickPlantForToday(): Pair<String, String> {
        val dayOfYear = SimpleDateFormat("D", Locale.US).format(Date()).toInt()
        val categories = preferredCategories.ifEmpty { PreferencesManager.ALL_CATEGORIES }
        val category = categories[dayOfYear % categories.size]
        val plants = CATEGORY_PLANTS[category] ?: ALL_PLANTS
        return plants[dayOfYear % plants.size]
    }

    private suspend fun fetchAndSave(
        dateKey: String,
        query: String,
        displayName: String,
        forceNew: Boolean = false
    ): DailyPlant {
        val photo = try {
            unsplashApi.getRandomPhoto(query = query)
        } catch (e: Exception) {
            Log.e(TAG, "Unsplash fetch failed for query '$query'", e)
            throw e
        }

        val location = buildLocationString(photo)
        val (insight, scientificName) = fetchBotanicalData(displayName)

        val plant = DailyPlant(
            dateKey = dateKey,
            photoId = photo.id,
            imageUrlFull = photo.urls.full,
            imageUrlRegular = photo.urls.regular,
            plantName = displayName,
            scientificName = scientificName,
            locationName = location,
            photographerName = photo.user.name,
            photographerUsername = photo.user.username,
            photographerProfileUrl = photo.user.links.html,
            downloadLocationUrl = photo.links.downloadLocation,
            botanicalInsight = insight
        )

        dao.insert(plant)
        if (!forceNew) pruneOldEntries()
        return plant
    }

    private suspend fun pruneOldEntries() {
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        dao.pruneOlderThan(sevenDaysAgo)
    }

    private fun buildLocationString(photo: UnsplashPhoto): String? {
        val loc = photo.location ?: return null
        return listOfNotNull(loc.name, loc.city, loc.country)
            .filter { it.isNotBlank() }
            .firstOrNull()
    }

    private suspend fun fetchBotanicalData(plantName: String): Pair<String, String?> {
        return try {
            val prompt = """For the plant species "$plantName", provide:
1. INSIGHT: One fascinating lesser-known botanical fact in exactly 2-3 sentences. Be specific and surprising. No markdown.
2. SCIENTIFIC: The scientific (Latin) binomial name only, e.g. "Rosa canina". If unknown, write "Unknown".

Format:
INSIGHT: [your insight here]
SCIENTIFIC: [scientific name here]"""

            val response = openAiApi.getChatCompletion(
                authorization = "Bearer ${BuildConfig.OPENAI_API_KEY}",
                request = ChatCompletionRequest(
                    messages = listOf(
                        ChatMessage(role = "system", content = "You are a botanist. Follow the format precisely."),
                        ChatMessage(role = "user", content = prompt)
                    ),
                    maxTokens = 200
                )
            )

            val text = response.choices.firstOrNull()?.message?.content?.trim() ?: ""
            val insightLine = text.lines().find { it.startsWith("INSIGHT:") }
                ?.removePrefix("INSIGHT:")?.trim()
            val scientificLine = text.lines().find { it.startsWith("SCIENTIFIC:") }
                ?.removePrefix("SCIENTIFIC:")?.trim()
                ?.takeIf { it != "Unknown" && it.isNotBlank() }

            Pair(insightLine ?: getFallbackInsight(plantName), scientificLine)
        } catch (e: Exception) {
            Log.w(TAG, "AI unavailable, using fallback", e)
            Pair(getFallbackInsight(plantName), null)
        }
    }

    suspend fun generateQuiz(plant: DailyPlant): QuizData? {
        return try {
            val scientificPart = if (!plant.scientificName.isNullOrBlank()) " (${plant.scientificName})" else ""
            val prompt = """Generate a multiple-choice quiz question about "${plant.plantName}"$scientificPart.
Return ONLY a valid JSON object, no markdown, no explanation:
{"question":"...","options":["...","...","...","..."],"correct":0,"explanation":"..."}
Rules: question is interesting botanical trivia, options has exactly 4 choices, correct is 0-3 index of right answer, explanation is 1-2 sentences."""

            val response = openAiApi.getChatCompletion(
                authorization = "Bearer ${BuildConfig.OPENAI_API_KEY}",
                request = ChatCompletionRequest(
                    messages = listOf(
                        ChatMessage(role = "system", content = "You are a botanist quiz master. Return only JSON."),
                        ChatMessage(role = "user", content = prompt)
                    ),
                    maxTokens = 300
                )
            )

            val raw = response.choices.firstOrNull()?.message?.content?.trim() ?: return null
            val jsonStr = raw.substringAfter("{").let { "{$it" }.substringBefore("}").let { "$it}" }
            val obj = JSONObject(jsonStr)
            val optionsArray = obj.getJSONArray("options")
            val options = (0 until 4).map { optionsArray.getString(it) }
            QuizData(
                question = obj.getString("question"),
                options = options,
                correct = obj.getInt("correct").coerceIn(0, 3),
                explanation = obj.getString("explanation"),
                dateKey = plant.dateKey
            )
        } catch (e: Exception) {
            Log.w(TAG, "Quiz generation failed", e)
            null
        }
    }

    private fun getFallbackInsight(plantName: String): String {
        val insights = listOf(
            "Plants communicate through an underground fungal network called the 'Wood Wide Web', sharing nutrients and chemical stress signals with neighbouring plants.",
            "Many flowering plants time their blooms to coincide with the peak activity of their specific pollinators — a relationship refined over millions of years of co-evolution.",
            "Some plants can detect the sound vibrations of caterpillars chewing their leaves and release defensive chemicals before any visible damage occurs.",
            "The oldest living plant on Earth is estimated to be a Posidonia australis seagrass clone in Australia, around 4,500 years old and stretching 180 km.",
            "Flowers that appear plain to human eyes often display vivid UV patterns invisible to us, acting as landing runways that guide bees directly to the nectar."
        )
        return insights[(plantName.hashCode() and Int.MAX_VALUE) % insights.size]
    }

    suspend fun triggerDownload(downloadUrl: String) {
        try { unsplashApi.triggerDownload(downloadUrl) } catch (e: Exception) {
            Log.w(TAG, "Download trigger failed", e)
        }
    }
}
