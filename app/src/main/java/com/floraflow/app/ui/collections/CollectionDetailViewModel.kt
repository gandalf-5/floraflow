package com.floraflow.app.ui.collections

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floraflow.app.data.AppDatabase
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantCollection
import com.floraflow.app.data.PlantCollectionCrossRef
import com.floraflow.app.data.PlantCollectionWithPlants
import kotlinx.coroutines.launch

class CollectionDetailViewModel(
    private val db: AppDatabase,
    val collectionId: Long
) : ViewModel() {

    private val _collectionWithPlants = MutableLiveData<PlantCollectionWithPlants?>()
    val collectionWithPlants: LiveData<PlantCollectionWithPlants?> = _collectionWithPlants

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _collectionWithPlants.value = db.plantCollectionDao().getCollectionWithPlants(collectionId)
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
