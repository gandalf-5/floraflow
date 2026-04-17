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
import com.floraflow.app.api.CareTipsRequest
import com.floraflow.app.api.CareTipsResponse
import com.floraflow.app.api.IdentifyRequest
import com.floraflow.app.api.RetrofitClient
import com.floraflow.app.util.LocaleUtil
import com.floraflow.app.data.AppDatabase
import com.floraflow.app.data.IdentificationRecord
import com.floraflow.app.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.util.Locale

sealed class IdentifyState {
    object Idle : IdentifyState()
    object Loading : IdentifyState()
    data class LimitReached(val limit: Int) : IdentifyState()
    data class Result(
        val commonName: String,
        val scientificName: String,
        val confidence: Int,
        val family: String?
    ) : IdentifyState()
    data class Error(val message: String) : IdentifyState()
}

class IdentifyViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PreferencesManager(app)

    private val _state = MutableLiveData<IdentifyState>(IdentifyState.Idle)
    val state: LiveData<IdentifyState> = _state

    private val _selectedImageUri = MutableLiveData<android.net.Uri?>()
    val selectedImageUri: LiveData<android.net.Uri?> = _selectedImageUri

    private val _dailyUsed = MutableLiveData<Int>(0)
    val dailyUsed: LiveData<Int> = _dailyUsed

    private val _isPremiumUser = MutableLiveData<Boolean>(false)
    val isPremiumUser: LiveData<Boolean> = _isPremiumUser

    private val _careTips = MutableLiveData<CareTipsResponse?>(null)
    val careTips: LiveData<CareTipsResponse?> = _careTips

    private val _careTipsLoading = MutableLiveData<Boolean>(false)
    val careTipsLoading: LiveData<Boolean> = _careTipsLoading

    init {
        viewModelScope.launch {
            val premium = prefs.isPremium.first()
            _isPremiumUser.value = premium
            if (!premium) refreshDailyCount()
        }
    }

    fun setImageUri(uri: android.net.Uri) {
        _selectedImageUri.value = uri
        _state.value = IdentifyState.Idle
    }

    fun identify(imageFile: File, latitude: Double? = null, longitude: Double? = null) {
        if (_state.value is IdentifyState.Loading) return
        _state.value = IdentifyState.Loading

        viewModelScope.launch {
            val isPremium = prefs.isPremium.first()

            if (!isPremium) {
                val today = LocalDate.now().toString()
                val savedDate = prefs.dailyIdDate.first()
                if (savedDate != today) {
                    prefs.setDailyIdCount(0)
                    prefs.setDailyIdDate(today)
                }
                val count = prefs.dailyIdCount.first()
                if (count >= PreferencesManager.FREE_DAILY_ID_LIMIT) {
                    _dailyUsed.value = count
                    _state.value = IdentifyState.LimitReached(PreferencesManager.FREE_DAILY_ID_LIMIT)
                    return@launch
                }
            }

            try {
                val imageBase64 = withContext(Dispatchers.IO) { compressAndEncode(imageFile) }

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.floraFlowApi.identify(IdentifyRequest(imageBase64 = imageBase64))
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

                    if (!isPremium) {
                        val newCount = (prefs.dailyIdCount.first()) + 1
                        prefs.setDailyIdCount(newCount)
                        prefs.setDailyIdDate(LocalDate.now().toString())
                        _dailyUsed.value = newCount
                    }

                    saveIdentification(imageFile, result, latitude, longitude)
                    fetchCareTips(result.commonName, result.scientificName)
                } else {
                    _state.value = IdentifyState.Error("Plant not recognized. Try a clearer photo.")
                }
            } catch (e: Exception) {
                Log.e("IdentifyVM", "Identification failed", e)
                _state.value = IdentifyState.Error("Could not identify plant. Check your connection.")
            }
        }
    }

    fun fetchCareTips(plantName: String, scientificName: String) {
        _careTips.value = null
        _careTipsLoading.value = true
        viewModelScope.launch {
            try {
                val tips = withContext(Dispatchers.IO) {
                    RetrofitClient.floraFlowApi.getPlantCare(
                        CareTipsRequest(
                            plantName = plantName,
                            scientificName = scientificName,
                            lang = LocaleUtil.getDeviceLang()
                        )
                    )
                }
                _careTips.value = tips
            } catch (e: Exception) {
                Log.e("IdentifyVM", "Care tips fetch failed", e)
            } finally {
                _careTipsLoading.value = false
            }
        }
    }

    fun reset() {
        _state.value = IdentifyState.Idle
        _selectedImageUri.value = null
        _careTips.value = null
        _careTipsLoading.value = false
    }

    private suspend fun refreshDailyCount() {
        val today = LocalDate.now().toString()
        val savedDate = prefs.dailyIdDate.first()
        val count = if (savedDate == today) prefs.dailyIdCount.first() else 0
        _dailyUsed.value = count
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

}
