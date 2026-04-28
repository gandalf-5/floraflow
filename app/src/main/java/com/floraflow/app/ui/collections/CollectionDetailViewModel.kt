package com.floraflow.app.ui.collections

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floraflow.app.data.AppDatabase
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantCollectionCrossRef
import com.floraflow.app.data.PlantCollectionWithPlants
import kotlinx.coroutines.launch

class CollectionDetailViewModel(
    private val db: AppDatabase,
    val collectionId: Long
) : ViewModel() {

    private val _collectionWithPlants = MutableLiveData<PlantCollectionWithPlants?>()
    val collectionWithPlants: LiveData<PlantCollectionWithPlants?> = _collectionWithPlants

    private val _availablePlants = MutableLiveData<List<DailyPlant>>(emptyList())
    val availablePlants: LiveData<List<DailyPlant>> = _availablePlants

    init { load() }

    fun load() {
        viewModelScope.launch {
            _collectionWithPlants.value = db.plantCollectionDao().getCollectionWithPlants(collectionId)
        }
    }

    /** Load discovered plants NOT already in this collection, for the picker. */
    fun loadAvailablePlants() {
        viewModelScope.launch {
            val all = db.dailyPlantDao().getHistory()
            val inCollection = (_collectionWithPlants.value?.plants ?: emptyList())
                .map { it.dateKey }.toSet()
            _availablePlants.value = all.filter { it.dateKey !in inCollection }
        }
    }

    fun addPlant(plant: DailyPlant) {
        viewModelScope.launch {
            db.plantCollectionDao().addPlantToCollection(
                PlantCollectionCrossRef(collectionId = collectionId, plantDateKey = plant.dateKey)
            )
            load()
        }
    }

    fun removePlant(plant: DailyPlant) {
        viewModelScope.launch {
            db.plantCollectionDao().removePlantFromCollection(
                PlantCollectionCrossRef(collectionId = collectionId, plantDateKey = plant.dateKey)
            )
            load()
        }
    }
}
