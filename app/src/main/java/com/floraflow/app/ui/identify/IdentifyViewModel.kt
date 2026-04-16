package com.floraflow.app.ui.identify

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floraflow.app.api.IdentifyRequest
import com.floraflow.app.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

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
                val imageBase64 = withContext(Dispatchers.IO) {
                    compressAndEncode(imageFile)
                }

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.floraFlowApi.identify(
                        IdentifyRequest(imageBase64 = imageBase64)
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

    /** Resize to max 800px and JPEG-compress to ~80% quality before Base64 encoding. */
    private fun compressAndEncode(file: File): String {
        val original = BitmapFactory.decodeFile(file.absolutePath)
        val maxSize = 800
        val scale = minOf(maxSize.toFloat() / original.width, maxSize.toFloat() / original.height, 1f)
        val scaled = if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            )
        } else original

        val out = ByteArrayOutputStream()
        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
        if (scaled !== original) scaled.recycle()
        original.recycle()
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
