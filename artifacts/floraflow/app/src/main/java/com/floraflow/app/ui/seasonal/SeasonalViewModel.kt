package com.floraflow.app.ui.seasonal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floraflow.app.api.FloraFlowApi
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.Calendar

data class CuratedPlant(
    val name: String,
    val imageUrl: String?,
    val photographer: String?
)

data class SeasonalSection(
    val season: String,
    val emoji: String,
    val color: String,
    val monthRange: String,
    val isCurrentSeason: Boolean,
    val plants: List<DailyPlant>,
    val curatedPlants: List<CuratedPlant>,
    val seasonFact: String
)

class SeasonalViewModel(
    private val repository: PlantRepository,
    private val floraFlowApi: FloraFlowApi
) : ViewModel() {

    private val _sections = MutableLiveData<List<SeasonalSection>>()
    val sections: LiveData<List<SeasonalSection>> = _sections

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    companion object {
        val SEASONAL_PLANTS = mapOf(
            "Spring" to listOf("Cherry Blossom", "Tulip", "Daffodil", "Magnolia", "Wisteria", "Lilac", "Peony", "Iris"),
            "Summer" to listOf("Sunflower", "Lavender", "Hibiscus", "Rose", "Lotus", "Bougainvillea", "Daisy", "Zinnia"),
            "Autumn" to listOf("Chrysanthemum", "Maple", "Aster", "Hydrangea", "Dahlia", "Goldenrod", "Sedum", "Helenium"),
            "Winter" to listOf("Poinsettia", "Holly", "Snowdrop", "Winter Jasmine", "Hellebore", "Camellia", "Cyclamen", "Witch Hazel")
        )
        val SEASON_MONTHS = mapOf(
            "Spring" to "March – May",
            "Summer" to "June – August",
            "Autumn" to "September – November",
            "Winter" to "December – February"
        )
        val SEASON_FACTS = mapOf(
            "Spring" to "Over 80% of the world's flowering plants bloom in spring — triggered by longer days and temperatures rising above 10 °C.",
            "Summer" to "Young sunflowers track the sun from east to west each day. Once mature, they stop and permanently face east — warming visiting bees on cool mornings.",
            "Autumn" to "Leaves don't gain colour in autumn. The green chlorophyll breaks down, revealing the yellows and oranges that were hidden inside the leaf all summer long.",
            "Winter" to "Some plants need weeks of cold (vernalization) to bloom the following spring. The freeze is not their enemy — it is their alarm clock."
        )
    }

    init { loadSeasonal() }

    private fun loadSeasonal() {
        viewModelScope.launch {
            _loading.value = true

            val all = repository.getAllForSeasonal()
            val bySeasonMap = mutableMapOf<String, MutableList<DailyPlant>>()
            for (plant in all) {
                val season = getSeason(plant.fetchedAt)
                bySeasonMap.getOrPut(season) { mutableListOf() }.add(plant)
            }

            val currentSeason = getSeason(System.currentTimeMillis())

            val orderedSeasons = listOf(
                Triple("Spring", "🌸", "#D8F3DC"),
                Triple("Summer", "☀️", "#FFF3B0"),
                Triple("Autumn", "🍂", "#FFD6A5"),
                Triple("Winter", "❄️", "#D0E8FF")
            ).sortedByDescending { (name, _, _) -> name == currentSeason }

            val sections = orderedSeasons.map { (name, emoji, color) ->
                val isCurrent = name == currentSeason
                val names = SEASONAL_PLANTS[name] ?: emptyList()
                val fetchCount = if (isCurrent) names.size else 4

                val curatedPlants = names.take(fetchCount).map { plantName ->
                    async {
                        try {
                            val resp = floraFlowApi.getCuratedPlant(plantName)
                            CuratedPlant(plantName, resp.imageUrl, resp.photographer)
                        } catch (e: Exception) {
                            CuratedPlant(plantName, null, null)
                        }
                    }
                }.awaitAll()

                SeasonalSection(
                    season = name,
                    emoji = emoji,
                    color = color,
                    monthRange = SEASON_MONTHS[name] ?: "",
                    isCurrentSeason = isCurrent,
                    plants = bySeasonMap[name] ?: emptyList(),
                    curatedPlants = curatedPlants,
                    seasonFact = SEASON_FACTS[name] ?: ""
                )
            }

            _sections.value = sections
            _loading.value = false
        }
    }

    private fun getSeason(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (cal.get(Calendar.MONTH) + 1) {
            3, 4, 5 -> "Spring"
            6, 7, 8 -> "Summer"
            9, 10, 11 -> "Autumn"
            else -> "Winter"
        }
    }
}
