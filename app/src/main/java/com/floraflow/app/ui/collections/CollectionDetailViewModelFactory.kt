package com.floraflow.app.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.floraflow.app.data.AppDatabase

class CollectionDetailViewModelFactory(
    private val db: AppDatabase,
    private val collectionId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CollectionDetailViewModel::class.java)) {
            return CollectionDetailViewModel(db, collectionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
