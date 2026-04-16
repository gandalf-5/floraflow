package com.floraflow.app.ui.identify

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floraflow.app.api.PlantNetApi
import com.floraflow.app.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

sealed class IdentifyState {
    object Idle : IdentifyState()
    object Loading : IdentifyState()
    data class Result(
        val commonName: String,
        val scientificName: String,
        val confidence: Int,
        val family: String?
    ) : IdentifyState()
    data class Error(val message: String) : IdentifyState()
}

class IdentifyViewModel : ViewModel() {

    private val _state = MutableLiveData<IdentifyState>(IdentifyState.Idle)
    val state: LiveData<IdentifyState> = _state

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    fun setImageUri(uri: Uri) {
        _selectedImageUri.value = uri
        _state.value = IdentifyState.Idle
    }

    fun identify(imageFile: File) {
        if (_state.value is IdentifyState.Loading) return
        _state.value = IdentifyState.Loading

        viewModelScope.launch {
            try {
                val requestBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("images", imageFile.name, requestBody)

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.plantNetApi.identify(
                        apiKey = PlantNetApi.API_KEY,
                        image = part
                    )
                }

                val best = response.results.firstOrNull()
                if (best != null) {
                    val confidence = (best.score * 100).toInt()
                    val common = best.species.commonNames.firstOrNull()
                        ?: best.species.scientificName
                    _state.value = IdentifyState.Result(
                        commonName = common,
                        scientificName = best.species.scientificName,
                        confidence = confidence,
                        family = best.species.family?.name
                    )
                } else {
                    _state.value = IdentifyState.Error("Plant not recognized. Try a clearer photo.")
                }
            } catch (e: Exception) {
                Log.e("IdentifyVM", "Identification failed", e)
                _state.value = IdentifyState.Error("Could not identify plant. Check your connection.")
            }
        }
    }

    fun reset() {
        _state.value = IdentifyState.Idle
        _selectedImageUri.value = null
    }
}
