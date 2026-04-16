package com.floraflow.app.ui.seasonal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.floraflow.app.data.PlantRepository

class SeasonalViewModelFactory(private val repository: PlantRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SeasonalViewModel(repository) as T
    }
}
