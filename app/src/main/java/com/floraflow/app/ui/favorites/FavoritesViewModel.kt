package com.floraflow.app.ui.favorites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.data.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val repository: PlantRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _plants = MutableLiveData<List<DailyPlant>>(emptyList())
    val plants: LiveData<List<DailyPlant>> = _plants

    private val _isPremium = MutableLiveData(false)
    val isPremium: LiveData<Boolean> = _isPremium

    private val _totalFavCount = MutableLiveData(0)
    val totalFavCount: LiveData<Int> = _totalFavCount

    init { load() }

    fun load() {
        viewModelScope.launch {
            val premium = prefs.isPremium.first()
            _isPremium.value = premium
            val all = repository.getFavorites()
            _totalFavCount.value = all.size
            _plants.value = if (premium) {
                all
            } else {
                all.take(PreferencesManager.FREE_FAVORITES_LIMIT)
            }
        }
    }

    fun removeFavorite(plant: DailyPlant) {
        viewModelScope.launch {
            repository.toggleFavorite(plant)
            load()
        }
    }
}
