package com.floraflow.app.ui.seasonal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantRepository
import kotlinx.coroutines.launch
import java.util.Calendar

data class SeasonalSection(
    val season: String,
    val emoji: String,
    val color: String,
    val monthRange: String,
    val isCurrentSeason: Boolean,
    val plants: List<DailyPlant>,
    val curatedNames: List<String>
)

class SeasonalViewModel(private val repository: PlantRepository) : ViewModel() {

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
            )

            val sorted = orderedSeasons.sortedByDescending { (name, _, _) -> name == currentSeason }

            val sections = sorted.map { (name, emoji, color) ->
                SeasonalSection(
                    season = name,
                    emoji = emoji,
                    color = color,
                    monthRange = SEASON_MONTHS[name] ?: "",
                    isCurrentSeason = name == currentSeason,
                    plants = bySeasonMap[name] ?: emptyList(),
                    curatedNames = SEASONAL_PLANTS[name] ?: emptyList()
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
