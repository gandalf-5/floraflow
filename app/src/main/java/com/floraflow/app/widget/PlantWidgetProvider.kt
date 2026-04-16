package com.floraflow.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.R
import com.floraflow.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlantWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_plant)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        views.setTextViewText(R.id.widget_plant_name, context.getString(R.string.app_name))
        views.setTextViewText(R.id.widget_subtitle, context.getString(R.string.todays_discovery))
        appWidgetManager.updateAppWidget(widgetId, views)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as FloraFlowApp
                val plant = app.database.dailyPlantDao().getByDate(
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        .format(java.util.Date())
                ) ?: return@launch

                val bitmap: Bitmap = Glide.with(context.applicationContext)
                    .asBitmap()
                    .load(plant.imageUrlRegular)
                    .submit(300, 300)
                    .get()

                withContext(Dispatchers.Main) {
                    views.setImageViewBitmap(R.id.widget_image, bitmap)
                    views.setTextViewText(R.id.widget_plant_name, plant.plantName)
                    if (!plant.scientificName.isNullOrBlank()) {
                        views.setTextViewText(R.id.widget_subtitle, plant.scientificName)
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                // Keep placeholder
            }
        }
    }
}
