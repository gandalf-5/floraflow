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
    val plants: List<DailyPlant>
)

class SeasonalViewModel(private val repository: PlantRepository) : ViewModel() {

    private val _sections = MutableLiveData<List<SeasonalSection>>()
    val sections: LiveData<List<SeasonalSection>> = _sections

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

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

            val orderedSeasons = listOf(
                Triple("Spring", "🌸", "#D8F3DC"),
                Triple("Summer", "☀️", "#FFF3B0"),
                Triple("Autumn", "🍂", "#FFD6A5"),
                Triple("Winter", "❄️", "#D0E8FF")
            )

            val sections = orderedSeasons.mapNotNull { (name, emoji, color) ->
                val plants = bySeasonMap[name] ?: return@mapNotNull null
                SeasonalSection(name, emoji, color, plants)
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
