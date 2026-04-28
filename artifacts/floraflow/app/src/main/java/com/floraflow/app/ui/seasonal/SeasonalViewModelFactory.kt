package com.floraflow.app.ui.seasonal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.floraflow.app.api.FloraFlowApi
import com.floraflow.app.data.PlantRepository

class SeasonalViewModelFactory(
    private val repository: PlantRepository,
    private val floraFlowApi: FloraFlowApi
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SeasonalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SeasonalViewModel(repository, floraFlowApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
