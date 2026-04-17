package com.floraflow.app.ui.identify

import android.app.Application
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.floraflow.app.api.IdentifyRequest
import com.floraflow.app.api.RetrofitClient
import com.floraflow.app.data.AppDatabase
import com.floraflow.app.data.IdentificationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

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

class IdentifyViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableLiveData<IdentifyState>(IdentifyState.Idle)
    val state: LiveData<IdentifyState> = _state

    private val _selectedImageUri = MutableLiveData<android.net.Uri?>()
    val selectedImageUri: LiveData<android.net.Uri?> = _selectedImageUri

    fun setImageUri(uri: android.net.Uri) {
        _selectedImageUri.value = uri
        _state.value = IdentifyState.Idle
    }

    fun identify(imageFile: File, latitude: Double? = null, longitude: Double? = null) {
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
                    val result = IdentifyState.Result(
                        commonName = common,
                        scientificName = best.species.scientificName,
                        confidence = confidence,
                        family = best.species.family?.name
                    )
                    _state.value = result
                    saveIdentification(imageFile, result, latitude, longitude)
                } else {
                    _state.value = IdentifyState.Error("Plant not recognized. Try a clearer photo.")
                }
            } catch (e: Exception) {
                Log.e("IdentifyVM", "Identification failed", e)
                _state.value = IdentifyState.Error("Could not identify plant. Check your connection.")
            }
        }
    }

    private fun saveIdentification(
        file: File,
        result: IdentifyState.Result,
        lat: Double?,
        lng: Double?
    ) {
        viewModelScope.launch {
            try {
                val permanentPath = withContext(Dispatchers.IO) { savePermanently(file) }
                val locName = if (lat != null && lng != null) reverseGeocode(lat, lng) else null
                val record = IdentificationRecord(
                    photoPath = permanentPath,
                    commonName = result.commonName,
                    scientificName = result.scientificName,
                    confidence = result.confidence,
                    family = result.family,
                    timestampMs = System.currentTimeMillis(),
                    latitude = lat,
                    longitude = lng,
                    locationName = locName
                )
                AppDatabase.getInstance(getApplication()).identificationRecordDao().insert(record)
            } catch (e: Exception) {
                Log.e("IdentifyVM", "Failed to save identification record", e)
            }
        }
    }

    private fun savePermanently(file: File): String {
        val dir = File(getApplication<Application>().filesDir, "identifications")
        dir.mkdirs()
        val dest = File(dir, "ident_${System.currentTimeMillis()}.jpg")
        file.copyTo(dest, overwrite = true)
        return dest.absolutePath
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(lat: Double, lng: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val addr = addresses?.firstOrNull() ?: return@withContext null
                listOfNotNull(addr.locality, addr.countryName)
                    .joinToString(", ")
                    .takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                null
            }
        }

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

    fun reset() {
        _state.value = IdentifyState.Idle
        _selectedImageUri.value = null
    }
}
