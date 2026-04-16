package com.floraflow.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
        val STREAK_COUNT = intPreferencesKey("streak_count")
        val LAST_OPEN_DATE = stringPreferencesKey("last_open_date")
        val DAILY_QUIZ_JSON = stringPreferencesKey("daily_quiz_json")

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

    val autoSyncWallpaper: Flow<Boolean> = context.dataStore.data.map { it[AUTO_SYNC_WALLPAPER] ?: false }
    val wallpaperHour: Flow<Int> = context.dataStore.data.map { it[WALLPAPER_HOUR] ?: 0 }
    val wallpaperTarget: Flow<Int> = context.dataStore.data.map { it[WALLPAPER_TARGET] ?: TARGET_ALL }
    val preferredCategories: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val saved = prefs[PREFERRED_CATEGORIES] ?: ""
        if (saved.isBlank()) ALL_CATEGORIES else saved.split("|").filter { it.isNotBlank() }
    }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val darkMode: Flow<Int> = context.dataStore.data.map { it[DARK_MODE] ?: DARK_MODE_SYSTEM }
    val streakCount: Flow<Int> = context.dataStore.data.map { it[STREAK_COUNT] ?: 0 }
    val lastOpenDate: Flow<String> = context.dataStore.data.map { it[LAST_OPEN_DATE] ?: "" }
    val dailyQuizJson: Flow<String> = context.dataStore.data.map { it[DAILY_QUIZ_JSON] ?: "" }

    suspend fun setAutoSyncWallpaper(enabled: Boolean) { context.dataStore.edit { it[AUTO_SYNC_WALLPAPER] = enabled } }
    suspend fun setWallpaperHour(hour: Int) { context.dataStore.edit { it[WALLPAPER_HOUR] = hour } }
    suspend fun setWallpaperTarget(target: Int) { context.dataStore.edit { it[WALLPAPER_TARGET] = target } }
    suspend fun setPreferredCategories(categories: List<String>) {
        context.dataStore.edit { it[PREFERRED_CATEGORIES] = categories.joinToString("|") }
    }
    suspend fun setNotificationsEnabled(enabled: Boolean) { context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled } }
    suspend fun setDarkMode(mode: Int) { context.dataStore.edit { it[DARK_MODE] = mode } }
    suspend fun setStreakCount(count: Int) { context.dataStore.edit { it[STREAK_COUNT] = count } }
    suspend fun setLastOpenDate(date: String) { context.dataStore.edit { it[LAST_OPEN_DATE] = date } }
    suspend fun setDailyQuizJson(json: String) { context.dataStore.edit { it[DAILY_QUIZ_JSON] = json } }
}
