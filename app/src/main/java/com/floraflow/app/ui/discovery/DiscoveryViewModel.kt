package com.floraflow.app.ui.discovery

import android.app.WallpaperManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.floraflow.app.api.CuriositiesRequest
import com.floraflow.app.api.RetrofitClient
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class DiscoveryUiState {
    object Loading : DiscoveryUiState()
    data class Success(val plant: DailyPlant) : DiscoveryUiState()
    data class Error(val message: String) : DiscoveryUiState()
}

sealed class WallpaperState {
    object Idle : WallpaperState()
    object Setting : WallpaperState()
    object Success : WallpaperState()
    data class Error(val message: String) : WallpaperState()
}

class DiscoveryViewModel(
    private val repository: PlantRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableLiveData<DiscoveryUiState>(DiscoveryUiState.Loading)
    val uiState: LiveData<DiscoveryUiState> = _uiState

    private val _wallpaperState = MutableLiveData<WallpaperState>(WallpaperState.Idle)
    val wallpaperState: LiveData<WallpaperState> = _wallpaperState

    private val _streakCount = MutableLiveData(0)
    val streakCount: LiveData<Int> = _streakCount

    // null = loading/not started, emptyList = error/unavailable, non-empty = ready to show
    private val _curiosities = MutableLiveData<List<String>?>(null)
    val curiosities: LiveData<List<String>?> = _curiosities

    init {
        loadTodayPlant()
        updateStreak()
    }

    fun loadTodayPlant() {
        _uiState.value = DiscoveryUiState.Loading
        _curiosities.value = null
        viewModelScope.launch {
            try {
                val plant = repository.fetchAndSaveTodayPlant()
                _uiState.value = DiscoveryUiState.Success(plant)
                fetchCuriosities(plant)
            } catch (e: Exception) {
                val cached = repository.getTodayPlant()
                if (cached != null) {
                    _uiState.value = DiscoveryUiState.Success(cached)
                    fetchCuriosities(cached)
                } else {
                    _uiState.value = DiscoveryUiState.Error(e.message ?: "Unable to load today's plant")
                    _curiosities.value = emptyList()
                }
            }
        }
    }

    fun refresh() = loadTodayPlant()

    fun refreshWithCategory(query: String) {
        _uiState.value = DiscoveryUiState.Loading
        _curiosities.value = null
        viewModelScope.launch {
            try {
                val plant = repository.fetchForCategory(query)
                _uiState.value = DiscoveryUiState.Success(plant)
                fetchCuriosities(plant)
            } catch (e: Exception) {
                _uiState.value = DiscoveryUiState.Error(e.message ?: "Unable to load plant")
                _curiosities.value = emptyList()
            }
        }
    }

    fun toggleFavorite(plant: DailyPlant) {
        viewModelScope.launch {
            repository.toggleFavorite(plant)
            val updated = repository.getPlantByKey(plant.dateKey)
            if (updated != null) _uiState.value = DiscoveryUiState.Success(updated)
        }
    }

    fun saveNotes(plant: DailyPlant, notes: String?) {
        viewModelScope.launch {
            repository.updateNotes(plant.dateKey, notes?.takeIf { it.isNotBlank() })
            val updated = repository.getPlantByKey(plant.dateKey)
            if (updated != null) _uiState.value = DiscoveryUiState.Success(updated)
        }
    }

    fun setAsWallpaper(context: Context, plant: DailyPlant) {
        if (_wallpaperState.value is WallpaperState.Setting) return
        _wallpaperState.value = WallpaperState.Setting
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    Glide.with(context.applicationContext).asBitmap().load(plant.imageUrlFull).submit().get()
                }
                withContext(Dispatchers.IO) {
                    WallpaperManager.getInstance(context.applicationContext).setBitmap(bitmap)
                }
                repository.triggerDownload(plant.downloadLocationUrl)
                _wallpaperState.value = WallpaperState.Success
            } catch (e: Exception) {
                Log.e("DiscoveryVM", "Wallpaper failed", e)
                _wallpaperState.value = WallpaperState.Error("Could not set wallpaper")
            }
        }
    }

    fun resetWallpaperState() { _wallpaperState.value = WallpaperState.Idle }

    private fun fetchCuriosities(plant: DailyPlant) {
        viewModelScope.launch {
            try {
                val lang = Locale.getDefault().language.let { if (it == "fr") "fr" else "en" }
                val response = RetrofitClient.floraFlowApi.getBotanicalCuriosities(
                    CuriositiesRequest(
                        plantName = plant.plantName,
                        scientificName = plant.scientificName,
                        lang = lang
                    )
                )
                val facts = response.facts.filter { it.isNotBlank() }
                _curiosities.value = facts
            } catch (e: Exception) {
                Log.w("DiscoveryVM", "Curiosities fetch failed: ${e.message}")
                _curiosities.value = emptyList()
            }
        }
    }

    private fun updateStreak() {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val lastDate = prefs.lastOpenDate.first()
            val currentStreak = prefs.streakCount.first()

            val yesterday = getYesterday()
            val newStreak = when {
                lastDate == today -> currentStreak.coerceAtLeast(1)
                lastDate == yesterday -> currentStreak + 1
                else -> 1
            }

            prefs.setStreakCount(newStreak)
            prefs.setLastOpenDate(today)
            _streakCount.value = newStreak
        }
    }

    private fun getYesterday(): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
}
