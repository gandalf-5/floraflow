package com.floraflow.app.ui.story

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floraflow.app.api.RetrofitClient
import com.floraflow.app.api.StoryRequest
import com.floraflow.app.api.StoryResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class StoryState {
    object Idle : StoryState()
    object Loading : StoryState()
    data class Success(val story: StoryResponse) : StoryState()
    data class Error(val message: String) : StoryState()
}

class StoryViewModel : ViewModel() {

    private val _state = MutableLiveData<StoryState>(StoryState.Idle)
    val state: LiveData<StoryState> = _state

    fun loadStory(plantName: String, scientificName: String?) {
        if (_state.value is StoryState.Loading) return
        _state.value = StoryState.Loading
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.floraFlowApi.getBotanicalStory(
                        StoryRequest(plantName = plantName, scientificName = scientificName)
                    )
                }
                _state.value = StoryState.Success(response)
            } catch (e: Exception) {
                _state.value = StoryState.Error("Could not load story. Check your connection.")
            }
        }
    }
}
