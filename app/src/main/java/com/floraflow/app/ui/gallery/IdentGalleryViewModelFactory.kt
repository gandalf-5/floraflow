package com.floraflow.app.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.floraflow.app.data.IdentificationRecordDao

class IdentGalleryViewModelFactory(
    private val dao: IdentificationRecordDao,
    private val isPremium: Boolean
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        IdentGalleryViewModel(dao, isPremium) as T
}
