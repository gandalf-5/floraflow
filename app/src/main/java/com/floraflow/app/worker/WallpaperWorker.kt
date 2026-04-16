package com.floraflow.app.worker

import android.app.WallpaperManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.content.Context
import com.bumptech.glide.Glide
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class WallpaperWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "WallpaperWorker"
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val app = applicationContext as FloraFlowApp
                val prefs = PreferencesManager(applicationContext)
                val target = prefs.wallpaperTarget.first()
                val categories = prefs.preferredCategories.first()

                val repository = PlantRepository(
                    app.database.dailyPlantDao(),
                    app.unsplashApi,
                    app.floraFlowApi,
                    categories
                )

                val plant = repository.fetchAndSaveTodayPlant()

                val bitmap = Glide.with(applicationContext)
                    .asBitmap()
                    .load(plant.imageUrlFull)
                    .submit()
                    .get()

                val wallpaperManager = WallpaperManager.getInstance(applicationContext)
                when (target) {
                    PreferencesManager.TARGET_HOME ->
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    PreferencesManager.TARGET_LOCK ->
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    else ->
                        wallpaperManager.setBitmap(bitmap)
                }

                repository.triggerDownload(plant.downloadLocationUrl)
                Log.d(TAG, "Wallpaper set: ${plant.plantName} (target=$target)")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set wallpaper", e)
                Result.retry()
            }
        }
    }
}
