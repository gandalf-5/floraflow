package com.floraflow.app.ui.discovery

import android.app.WallpaperManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

class DiscoveryViewModel(private val repository: PlantRepository) : ViewModel() {

    companion object {
        private const val TAG = "DiscoveryViewModel"
    }

    private val _uiState = MutableLiveData<DiscoveryUiState>(DiscoveryUiState.Loading)
    val uiState: LiveData<DiscoveryUiState> = _uiState

    private val _wallpaperState = MutableLiveData<WallpaperState>(WallpaperState.Idle)
    val wallpaperState: LiveData<WallpaperState> = _wallpaperState

    init {
        loadTodayPlant()
    }

    fun loadTodayPlant() {
        _uiState.value = DiscoveryUiState.Loading
        viewModelScope.launch {
            try {
                val plant = repository.fetchAndSaveTodayPlant()
                _uiState.value = DiscoveryUiState.Success(plant)
            } catch (e: Exception) {
                val cached = repository.getTodayPlant()
                if (cached != null) {
                    _uiState.value = DiscoveryUiState.Success(cached)
                } else {
                    _uiState.value = DiscoveryUiState.Error(
                        e.message ?: "Unable to load today's plant"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadTodayPlant()
    }

    fun setAsWallpaper(context: Context, plant: DailyPlant) {
        if (_wallpaperState.value is WallpaperState.Setting) return

        _wallpaperState.value = WallpaperState.Setting
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    Glide.with(context.applicationContext)
                        .asBitmap()
                        .load(plant.imageUrlFull)
                        .submit()
                        .get()
                }

                withContext(Dispatchers.IO) {
                    val wallpaperManager = WallpaperManager.getInstance(context.applicationContext)
                    wallpaperManager.setBitmap(bitmap)
                }

                repository.triggerDownload(plant.downloadLocationUrl)

                Log.d(TAG, "Wallpaper set: ${plant.plantName}")
                _wallpaperState.value = WallpaperState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set wallpaper", e)
                _wallpaperState.value = WallpaperState.Error("Could not set wallpaper")
            }
        }
    }

    fun resetWallpaperState() {
        _wallpaperState.value = WallpaperState.Idle
    }
}
