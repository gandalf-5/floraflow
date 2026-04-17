package com.floraflow.app.ui.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.floraflow.app.data.IdentificationRecord
import com.floraflow.app.data.IdentificationRecordDao
import com.floraflow.app.data.PreferencesManager

class IdentGalleryViewModel(
    private val dao: IdentificationRecordDao,
    private val isPremium: Boolean
) : ViewModel() {

    val records: LiveData<List<IdentificationRecord>> =
        if (isPremium) dao.getAllRecords().asLiveData()
        else dao.getRecentRecords(PreferencesManager.FREE_DAILY_ID_LIMIT * 10).asLiveData()
}
