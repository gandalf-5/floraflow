package com.floraflow.app.worker

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.data.PlantRepository
import kotlinx.coroutines.Dispatchers
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
                val repository = PlantRepository(
                    app.database.dailyPlantDao(),
                    app.unsplashApi,
                    app.openAiApi
                )

                val plant = repository.fetchAndSaveTodayPlant()

                val bitmap = withContext(Dispatchers.IO) {
                    Glide.with(applicationContext)
                        .asBitmap()
                        .load(plant.imageUrlFull)
                        .submit()
                        .get()
                }

                val wallpaperManager = WallpaperManager.getInstance(applicationContext)
                wallpaperManager.setBitmap(bitmap)

                repository.triggerDownload(plant.downloadLocationUrl)

                Log.d(TAG, "Wallpaper set successfully for ${plant.plantName}")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set wallpaper", e)
                Result.retry()
            }
        }
    }
}
