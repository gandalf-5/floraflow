package com.floraflow.app.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantRepository
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: PlantRepository) : ViewModel() {

    private val _plants = MutableLiveData<List<DailyPlant>>(emptyList())
    val plants: LiveData<List<DailyPlant>> = _plants

    init { load() }

    fun load() {
        viewModelScope.launch {
            _plants.value = repository.getHistory()
        }
    }

    fun toggleFavorite(plant: DailyPlant) {
        viewModelScope.launch {
            repository.toggleFavorite(plant)
            load()
        }
    }
}
