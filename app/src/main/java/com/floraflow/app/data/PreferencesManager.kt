package com.floraflow.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "floraflow_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val AUTO_SYNC_WALLPAPER = booleanPreferencesKey("auto_sync_wallpaper")
    }

    val autoSyncWallpaper: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_SYNC_WALLPAPER] ?: false
    }

    suspend fun setAutoSyncWallpaper(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_SYNC_WALLPAPER] = enabled
        }
    }
}
