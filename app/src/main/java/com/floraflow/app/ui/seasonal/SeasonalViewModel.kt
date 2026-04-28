package com.floraflow.app.ui.seasonal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantRepository

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

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    companion object {
        private data class EcosystemSpec(
            val name: String,
            val emoji: String,
            val color: String,
            val description: String,
            val plants: List<String>
        )

        private val ECOSYSTEMS = listOf(
            EcosystemSpec(
                name = "Forêts tropicales",
                emoji = "🌿",
                color = "#2D6A4F",
                description = "Amériques · Asie · Afrique",
                plants = listOf(
                    "Bird of Paradise", "Frangipani", "Swiss Cheese Plant",
                    "Flamingo Lily", "Jade Vine", "Black Bat Plant",
                    "Traveller's Palm", "Indian Shot", "Lobster Claw Heliconia"
                )
            ),
            EcosystemSpec(
                name = "Prairies sauvages",
                emoji = "🌸",
                color = "#52B788",
                description = "Europe · Amérique du Nord",
                plants = listOf(
                    "Common Poppy", "Cornflower", "Wild Pansy",
                    "Meadowsweet", "Harebell", "Meadow Cranesbill",
                    "Red Campion", "Ragged Robin", "Field Scabious"
                )
            ),
            EcosystemSpec(
                name = "Déserts & Steppes",
                emoji = "🌵",
                color = "#B5834A",
                description = "Mexique · Sahara · Asie centrale",
                plants = listOf(
                    "Saguaro Cactus", "Prickly Pear", "Organ Pipe Cactus",
                    "Desert Rose", "Joshua Tree Yucca", "Desert Marigold",
                    "Barrel Cactus", "Globe Thistle"
                )
            ),
            EcosystemSpec(
                name = "Jardins d'Asie",
                emoji = "🌺",
                color = "#C0547A",
                description = "Japon · Chine · Corée · Inde",
                plants = listOf(
                    "Cherry Blossom", "Sacred Lotus", "Chinese Peony",
                    "Star Magnolia", "Japanese Camellia", "Chinese Wisteria",
                    "Bearded Iris", "Grape Hyacinth", "Chrysanthemum"
                )
            ),
            EcosystemSpec(
                name = "Méditerranée",
                emoji = "🌊",
                color = "#2A7EA8",
                description = "Provence · Grèce · Maroc",
                plants = listOf(
                    "Lavender", "Common Poppy", "Rock Rose",
                    "Judas Tree", "Poppy Anemone", "Common Rosemary",
                    "Bougainvillea", "Spanish Lavender", "Oleander"
                )
            ),
            EcosystemSpec(
                name = "Montagnes alpines",
                emoji = "🏔️",
                color = "#5B7FA6",
                description = "Alpes · Himalaya · Andes",
                plants = listOf(
                    "Edelweiss", "Alpine Gentian", "Glacier Crowfoot",
                    "Alpine Aster", "Mountain Avens", "Star Magnolia",
                    "Crown Imperial", "Spring Crocus", "Hepatica"
                )
            )
        )
    }

    init {
        buildSections()
    }

    private fun buildSections() {
        _loading.value = false
        val result = ECOSYSTEMS.map { eco ->
            SeasonalSection(
                season = eco.name,
                emoji = eco.emoji,
                color = eco.color,
                monthRange = eco.description,
                isCurrentSeason = false,
                plants = emptyList(),
                curatedNames = eco.plants
            )
        }
        _sections.value = result
    }
}

class SeasonalViewModelFactory(
    private val repository: PlantRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SeasonalViewModel(repository) as T
}
