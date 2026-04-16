package com.floraflow.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "floraflow_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val AUTO_SYNC_WALLPAPER = booleanPreferencesKey("auto_sync_wallpaper")
        val WALLPAPER_HOUR = intPreferencesKey("wallpaper_hour")
        val WALLPAPER_TARGET = intPreferencesKey("wallpaper_target")
        val PREFERRED_CATEGORIES = stringPreferencesKey("preferred_categories")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DARK_MODE = intPreferencesKey("dark_mode")

        const val TARGET_ALL = 0
        const val TARGET_HOME = 1
        const val TARGET_LOCK = 2

        const val DARK_MODE_SYSTEM = 0
        const val DARK_MODE_OFF = 1
        const val DARK_MODE_ON = 2

        val ALL_CATEGORIES = listOf(
            "tropical plant", "wildflower meadow", "fern forest",
            "succulent garden", "orchid", "bonsai tree", "botanical garden",
            "moss forest", "water lily", "cactus desert", "cherry blossom",
            "lavender field", "sunflower", "magnolia tree", "lotus flower"
        )

        val DISPLAY_CATEGORIES = listOf(
            "Tropical", "Wildflower", "Fern", "Succulent",
            "Orchid", "Bonsai", "Botanical", "Moss",
            "Water Lily", "Cactus", "Cherry Blossom",
            "Lavender", "Sunflower", "Magnolia", "Lotus"
        )
    }

    val autoSyncWallpaper: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_SYNC_WALLPAPER] ?: false
    }

    val wallpaperHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[WALLPAPER_HOUR] ?: 0
    }

    val wallpaperTarget: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[WALLPAPER_TARGET] ?: TARGET_ALL
    }

    val preferredCategories: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val saved = prefs[PREFERRED_CATEGORIES] ?: ""
        if (saved.isBlank()) ALL_CATEGORIES else saved.split("|").filter { it.isNotBlank() }
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NOTIFICATIONS_ENABLED] ?: true
    }

    val darkMode: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE] ?: DARK_MODE_SYSTEM
    }

    suspend fun setAutoSyncWallpaper(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[AUTO_SYNC_WALLPAPER] = enabled }
    }

    suspend fun setWallpaperHour(hour: Int) {
        context.dataStore.edit { prefs -> prefs[WALLPAPER_HOUR] = hour }
    }

    suspend fun setWallpaperTarget(target: Int) {
        context.dataStore.edit { prefs -> prefs[WALLPAPER_TARGET] = target }
    }

    suspend fun setPreferredCategories(categories: List<String>) {
        context.dataStore.edit { prefs -> prefs[PREFERRED_CATEGORIES] = categories.joinToString("|") }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setDarkMode(mode: Int) {
        context.dataStore.edit { prefs -> prefs[DARK_MODE] = mode }
    }
}
