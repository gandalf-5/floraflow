package com.floraflow.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.data.PreferencesManager

class FavoritesViewModelFactory(
    private val repository: PlantRepository,
    private val prefs: PreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FavoritesViewModel(repository, prefs) as T
    }
}
