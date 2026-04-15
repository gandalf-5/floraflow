package com.floraflow.app.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.floraflow.app.data.PlantRepository

class DiscoveryViewModelFactory(private val repository: PlantRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiscoveryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiscoveryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
