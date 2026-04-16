package com.floraflow.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.floraflow.app.data.DailyPlant
import java.io.File

object StoryShareUtil {

    fun share(context: Context, plant: DailyPlant, plantBitmap: Bitmap) {
        try {
            val w = 1080
            val h = 1920
            val story = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(story)

            // Background: scaled plant image
            val scaled = Bitmap.createScaledBitmap(plantBitmap, w, h, true)
            canvas.drawBitmap(scaled, 0f, 0f, null)
            scaled.recycle()

            // Dark gradient overlay (bottom 60%)
            val gradientPaint = Paint()
            gradientPaint.shader = LinearGradient(
                0f, h * 0.3f, 0f, h.toFloat(),
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#CC000000"), Color.parseColor("#EE000000")),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, h * 0.3f, w.toFloat(), h.toFloat(), gradientPaint)

            // Top branding bar
            val topBarPaint = Paint().apply {
                color = Color.parseColor("#CC1B2E1F")
                isAntiAlias = true
            }
            canvas.drawRect(0f, 0f, w.toFloat(), 160f, topBarPaint)

            val brandPaint = Paint().apply {
                color = Color.WHITE
                textSize = 52f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.1f
            }
            canvas.drawText("🌿 FloraFlow", 60f, 110f, brandPaint)

            // Plant name (large)
            val namePaint = Paint().apply {
                color = Color.WHITE
                textSize = 96f
                isAntiAlias = true
                typeface = Typeface.SERIF
            }
            drawMultilineText(canvas, plant.plantName, namePaint, 60f, h - 800f, w - 120)

            // Scientific name
            if (!plant.scientificName.isNullOrBlank()) {
                val sciPaint = Paint().apply {
                    color = Color.parseColor("#74C69D")
                    textSize = 52f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                }
                canvas.drawText(plant.scientificName, 60f, h - 680f, sciPaint)
            }

            // Divider
            val divPaint = Paint().apply {
                color = Color.parseColor("#8052B788")
                strokeWidth = 2f
            }
            canvas.drawLine(60f, h - 630f, 300f, h - 630f, divPaint)

            // Insight text (truncated)
            val insightPaint = Paint().apply {
                color = Color.parseColor("#DDFFFFFF")
                textSize = 40f
                isAntiAlias = true
            }
            val truncatedInsight = if (plant.botanicalInsight.length > 120)
                plant.botanicalInsight.take(117) + "…"
            else plant.botanicalInsight
            drawMultilineText(canvas, truncatedInsight, insightPaint, 60f, h - 580f, w - 120)

            // Photographer credit
            val creditPaint = Paint().apply {
                color = Color.parseColor("#99FFFFFF")
                textSize = 36f
                isAntiAlias = true
            }
            canvas.drawText("📷 ${plant.photographerName} · Unsplash", 60f, h - 100f, creditPaint)

            // Save and share
            val dir = File(context.cacheDir, "story").also { it.mkdirs() }
            val file = File(dir, "floraflow_story.jpg")
            file.outputStream().use { story.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            story.recycle()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "#FloraFlow #Botany #${plant.plantName.replace(" ", "")}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share as Story"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drawMultilineText(
        canvas: Canvas, text: String, paint: Paint,
        x: Float, startY: Float, maxWidth: Int
    ) {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        val bounds = Rect()
        for (word in words) {
            val test = if (currentLine.isEmpty()) word else "$currentLine $word"
            paint.getTextBounds(test, 0, test.length, bounds)
            if (bounds.width() > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = test
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        val lineHeight = paint.textSize * 1.4f
        lines.forEachIndexed { i, line ->
            canvas.drawText(line, x, startY + i * lineHeight, paint)
        }
    }
}
