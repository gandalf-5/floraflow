package com.floraflow.app.ui.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.data.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: PlantRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _plants = MutableLiveData<List<DailyPlant>>(emptyList())
    val plants: LiveData<List<DailyPlant>> = _plants

    private val _isPremium = MutableLiveData(false)
    val isPremium: LiveData<Boolean> = _isPremium

    init { load() }

    fun load() {
        viewModelScope.launch {
            val premium = prefs.isPremium.first()
            _isPremium.value = premium
            _plants.value = if (premium) {
                repository.getHistory()
            } else {
                repository.getHistoryLast7Days()
            }
        }
    }

    fun toggleFavorite(plant: DailyPlant) {
        viewModelScope.launch {
            repository.toggleFavorite(plant)
            load()
        }
    }
}
