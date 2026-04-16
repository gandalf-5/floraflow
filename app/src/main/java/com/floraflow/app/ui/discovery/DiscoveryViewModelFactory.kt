package com.floraflow.app.ui.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.data.PreferencesManager

class DiscoveryViewModelFactory(
    private val repository: PlantRepository,
    private val prefs: PreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DiscoveryViewModel(repository, prefs) as T
    }
}
