package com.floraflow.app.ui.collections

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.floraflow.app.data.AppDatabase
import com.floraflow.app.data.PlantCollection
import com.floraflow.app.data.PlantCollectionCrossRef
import com.floraflow.app.util.BadgeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CollectionsViewModel(private val db: AppDatabase) : ViewModel() {

    val collections: LiveData<List<PlantCollection>> =
        db.plantCollectionDao().getAllCollectionsFlow().asLiveData()

    private val _collectionPlantCounts = MutableLiveData<Map<Long, Int>>(emptyMap())
    val collectionPlantCounts: LiveData<Map<Long, Int>> = _collectionPlantCounts

    init {
        loadCounts()
    }

    fun loadCounts() {
        viewModelScope.launch {
            val cols = db.plantCollectionDao().getAllCollectionsFlow().first()
            val counts = cols.associate { col ->
                col.id to db.plantCollectionDao().getPlantCount(col.id)
            }
            _collectionPlantCounts.value = counts
        }
    }

    fun createCollection(name: String, emoji: String) {
        viewModelScope.launch {
            val newId = db.plantCollectionDao().insertCollection(
                PlantCollection(name = name.trim(), emoji = emoji.trim().ifBlank { "🌿" })
            )
            // Check badge for collections
            val colCount = db.plantCollectionDao().getCollectionCount()
            BadgeManager.checkAndAwardBadges(db, collectionCount = colCount)
            loadCounts()
        }
    }

    fun deleteCollection(collection: PlantCollection) {
        viewModelScope.launch {
            db.plantCollectionDao().deleteCollection(collection)
            loadCounts()
        }
    }

    fun addPlantToCollection(dateKey: String, collectionId: Long) {
        viewModelScope.launch {
            db.plantCollectionDao().addPlantToCollection(
                PlantCollectionCrossRef(collectionId = collectionId, plantDateKey = dateKey)
            )
            loadCounts()
        }
    }

    suspend fun getCollectionsForPlant(dateKey: String) =
        db.plantCollectionDao().getCollectionsForPlant(dateKey)
}
