package com.floraflow.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.R
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.data.PreferencesManager
import com.floraflow.app.ui.MainActivity
import kotlinx.coroutines.flow.first

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "floraflow_daily"
        const val NOTIFICATION_ID = 1001

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Daily Plant Discovery",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Your daily botanical discovery notification"
                }
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        }
    }

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager(applicationContext)
        val notificationsEnabled = prefs.notificationsEnabled.first()
        if (!notificationsEnabled) return Result.success()

        return try {
            val app = applicationContext as FloraFlowApp
            val categories = prefs.preferredCategories.first()
            val repository = PlantRepository(
                app.database.dailyPlantDao(), app.unsplashApi, app.openAiApi, categories
            )
            val plant = repository.getTodayPlant() ?: return Result.success()

            createChannel(applicationContext)

            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_discover)
                .setContentTitle("🌿 Today's Plant: ${plant.plantName}")
                .setContentText(plant.botanicalInsight.take(100) + "…")
                .setStyle(NotificationCompat.BigTextStyle().bigText(plant.botanicalInsight))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
