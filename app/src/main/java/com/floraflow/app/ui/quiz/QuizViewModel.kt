package com.floraflow.app.ui.quiz

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.data.PreferencesManager
import com.floraflow.app.data.QuizData
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class QuizUiState {
    object Loading : QuizUiState()
    data class Ready(val quiz: QuizData, val selected: Int = -1, val revealed: Boolean = false) : QuizUiState()
    object Unavailable : QuizUiState()
}

class QuizViewModel(
    private val repository: PlantRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _state = MutableLiveData<QuizUiState>(QuizUiState.Loading)
    val state: LiveData<QuizUiState> = _state

    private val gson = Gson()

    init { loadOrGenerateQuiz() }

    private fun loadOrGenerateQuiz() {
        viewModelScope.launch {
            try {
                val today = repository.getTodayKey()
                val savedJson = prefs.dailyQuizJson.first()

                if (savedJson.isNotBlank()) {
                    try {
                        val saved = gson.fromJson(savedJson, QuizData::class.java)
                        if (saved.dateKey == today) {
                            _state.value = QuizUiState.Ready(saved)
                            return@launch
                        }
                    } catch (e: Exception) { /* parse error, regenerate */ }
                }

                val plant = try {
                    repository.fetchAndSaveTodayPlant()
                } catch (e: Exception) {
                    repository.getTodayPlant()
                }
                if (plant == null) {
                    _state.value = QuizUiState.Unavailable
                    return@launch
                }

                val quiz = repository.generateQuiz(plant)
                if (quiz != null) {
                    prefs.setDailyQuizJson(gson.toJson(quiz))
                    _state.value = QuizUiState.Ready(quiz)
                } else {
                    _state.value = QuizUiState.Unavailable
                }
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error", e)
                _state.value = QuizUiState.Unavailable
            }
        }
    }

    fun selectAnswer(index: Int) {
        val current = _state.value as? QuizUiState.Ready ?: return
        if (current.revealed) return
        _state.value = current.copy(selected = index, revealed = true)
    }

    fun retry() {
        _state.value = QuizUiState.Loading
        viewModelScope.launch {
            val plant = try {
                repository.fetchAndSaveTodayPlant()
            } catch (e: Exception) {
                repository.getTodayPlant()
            }
            if (plant == null) { _state.value = QuizUiState.Unavailable; return@launch }
            val quiz = repository.generateQuiz(plant)
            if (quiz != null) {
                prefs.setDailyQuizJson(Gson().toJson(quiz))
                _state.value = QuizUiState.Ready(quiz)
            } else {
                _state.value = QuizUiState.Unavailable
            }
        }
    }
}
