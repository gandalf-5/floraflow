package com.floraflow.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.floraflow.app.api.RetrofitClient
import com.floraflow.app.data.AppDatabase
import com.floraflow.app.data.PreferencesManager
import com.floraflow.app.worker.NotificationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FloraFlowApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val unsplashApi by lazy { RetrofitClient.unsplashApi }
    val openAiApi by lazy { RetrofitClient.openAiApi }

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationWorker.createChannel(this)
        applyDarkModePref()
    }

    private fun applyDarkModePref() {
        CoroutineScope(Dispatchers.Main).launch {
            val prefs = PreferencesManager(applicationContext)
            val mode = prefs.darkMode.first()
            AppCompatDelegate.setDefaultNightMode(
                when (mode) {
                    PreferencesManager.DARK_MODE_ON -> AppCompatDelegate.MODE_NIGHT_YES
                    PreferencesManager.DARK_MODE_OFF -> AppCompatDelegate.MODE_NIGHT_NO
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
            )
        }
    }

    companion object {
        lateinit var instance: FloraFlowApp
            private set
    }
}
