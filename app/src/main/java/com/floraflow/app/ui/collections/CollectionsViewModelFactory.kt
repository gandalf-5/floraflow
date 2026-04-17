package com.floraflow.app.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.floraflow.app.data.AppDatabase

class CollectionsViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectionsViewModel::class.java)) {
            return CollectionsViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
