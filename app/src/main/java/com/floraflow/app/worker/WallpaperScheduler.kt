package com.floraflow.app.worker

import android.content.Context
import androidx.work.*
import com.floraflow.app.data.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WallpaperScheduler {

    private const val WORK_TAG = "floraflow_wallpaper_sync"
    private const val NOTIFY_TAG = "floraflow_notification"

    fun schedule(context: Context) {
        val prefs = PreferencesManager(context)
        val hour = runBlocking { prefs.wallpaperHour.first() }
        val intervalMinutes = runBlocking { prefs.wallpaperIntervalMinutes.first() }

        val now = Calendar.getInstance()

        // For intervals < 24h (premium): fire at next multiple of the interval from midnight
        // For 24h (free): fire at the user-chosen hour
        val delay: Long = if (intervalMinutes < PreferencesManager.INTERVAL_24H) {
            val msPerInterval = intervalMinutes.toLong() * 60 * 1000L
            val msNow = now.timeInMillis
            val nextSlot = ((msNow / msPerInterval) + 1) * msPerInterval
            nextSlot - msNow
        } else {
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            target.timeInMillis - now.timeInMillis
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val wallpaperWork = PeriodicWorkRequestBuilder<WallpaperWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_TAG, ExistingPeriodicWorkPolicy.UPDATE, wallpaperWork
        )

        // Notification stays daily
        val dailyDelay = run {
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            target.timeInMillis - now.timeInMillis
        }
        val notifyWork = PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(dailyDelay, TimeUnit.MILLISECONDS)
            .addTag(NOTIFY_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOTIFY_TAG, ExistingPeriodicWorkPolicy.UPDATE, notifyWork
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
        WorkManager.getInstance(context).cancelAllWorkByTag(NOTIFY_TAG)
    }
}
