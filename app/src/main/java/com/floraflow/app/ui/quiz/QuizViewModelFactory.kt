package com.floraflow.app.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.data.PreferencesManager

class QuizViewModelFactory(
    private val repository: PlantRepository,
    private val prefs: PreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return QuizViewModel(repository, prefs) as T
    }
}
