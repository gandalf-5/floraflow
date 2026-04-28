package com.floraflow.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.floraflow.app.data.DailyPlant
import java.io.File

object StoryShareUtil {

    fun share(context: Context, plant: DailyPlant, plantBitmap: Bitmap) {
        try {
            val w = 1080
            val h = 1920

            // Extract vibrant color from the image for dynamic theming
            val palette = Palette.from(plantBitmap).generate()
            val vibrant = palette.getVibrantColor(
                palette.getDominantColor(Color.parseColor("#2D6A4F"))
            )
            val accentColor = vibrant
            val deepColor = ColorUtils.blendARGB(vibrant, Color.BLACK, 0.72f)
            val midColor  = ColorUtils.blendARGB(vibrant, Color.BLACK, 0.55f)

            val story = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(story)

            // ── 1. Background: plant photo cropped to center ──────────────────
            val srcRatio = plantBitmap.width.toFloat() / plantBitmap.height.toFloat()
            val dstRatio = w.toFloat() / h.toFloat()
            val (srcLeft, srcTop, srcW, srcH) = if (srcRatio > dstRatio) {
                val newW = (plantBitmap.height * dstRatio).toInt()
                val offset = (plantBitmap.width - newW) / 2
                listOf(offset, 0, newW, plantBitmap.height)
            } else {
                val newH = (plantBitmap.width / dstRatio).toInt()
                val offset = (plantBitmap.height - newH) / 2
                listOf(0, offset, plantBitmap.width, newH)
            }
            val src = android.graphics.Rect(srcLeft, srcTop, srcLeft + srcW, srcTop + srcH)
            val dst = android.graphics.Rect(0, 0, w, h)
            canvas.drawBitmap(plantBitmap, src, dst, null)

            // ── 2. Gradient overlay: transparent → deep color ─────────────────
            val gradPaint = Paint()
            gradPaint.shader = LinearGradient(
                0f, h * 0.28f, 0f, h.toFloat(),
                intArrayOf(Color.TRANSPARENT, ColorUtils.setAlphaComponent(deepColor, 180),
                    ColorUtils.setAlphaComponent(deepColor, 235), deepColor),
                floatArrayOf(0f, 0.35f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, h * 0.28f, w.toFloat(), h.toFloat(), gradPaint)

            // ── 3. Decorative rings (top-right corner) ────────────────────────
            val ringPaint = Paint().apply {
                color = ColorUtils.setAlphaComponent(accentColor, 55)
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            val cx = w + 80f; val cy = -80f
            canvas.drawCircle(cx, cy, 340f, ringPaint)
            canvas.drawCircle(cx, cy, 260f, ringPaint)
            canvas.drawCircle(cx, cy, 180f, ringPaint)
            // Filled inner accent dot
            val dotPaint = Paint().apply {
                color = ColorUtils.setAlphaComponent(accentColor, 90)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(cx, cy, 100f, dotPaint)

            // ── 4. Top branding pill ──────────────────────────────────────────
            val pillPaint = Paint().apply {
                color = ColorUtils.setAlphaComponent(deepColor, 200)
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(48f, 60f, 380f, 136f), 40f, 40f, pillPaint)

            val brandPaint = Paint().apply {
                color = Color.WHITE
                textSize = 46f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.06f
                setShadowLayer(8f, 0f, 2f, ColorUtils.setAlphaComponent(deepColor, 180))
            }
            canvas.drawText("🌿 FloraFlow", 80f, 112f, brandPaint)

            // ── 5. Location badge ─────────────────────────────────────────────
            var textBaseY = h * 0.685f
            val location = plant.locationName?.takeIf { it.isNotBlank() }
            if (location != null) {
                val locPaint = Paint().apply {
                    color = ColorUtils.setAlphaComponent(Color.WHITE, 220)
                    isAntiAlias = true
                }
                val locTextPaint = Paint().apply {
                    color = deepColor
                    textSize = 36f
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT_BOLD
                    letterSpacing = 0.08f
                }
                val locText = "📍 ${location.uppercase()}"
                val locWidth = locTextPaint.measureText(locText)
                val pillLeft = 60f; val pillRight = pillLeft + locWidth + 40f
                val pillTop = textBaseY - 52f; val pillBottom = textBaseY + 8f
                canvas.drawRoundRect(RectF(pillLeft, pillTop, pillRight, pillBottom), 28f, 28f, locPaint)
                canvas.drawText(locText, pillLeft + 20f, textBaseY - 10f, locTextPaint)
                textBaseY += 76f
            }

            // ── 6. Plant name ─────────────────────────────────────────────────
            val namePaint = Paint().apply {
                color = Color.WHITE
                textSize = if (plant.plantName.length > 14) 88f else 104f
                isAntiAlias = true
                typeface = Typeface.SERIF
                setShadowLayer(18f, 0f, 4f, ColorUtils.setAlphaComponent(deepColor, 200))
            }
            val nameLines = breakText(plant.plantName, namePaint, w - 120)
            nameLines.forEachIndexed { i, line ->
                canvas.drawText(line, 60f, textBaseY + i * namePaint.textSize * 1.25f, namePaint)
            }
            textBaseY += nameLines.size * namePaint.textSize * 1.25f + 20f

            // ── 7. Scientific name ────────────────────────────────────────────
            if (!plant.scientificName.isNullOrBlank()) {
                val sciPaint = Paint().apply {
                    color = ColorUtils.blendARGB(accentColor, Color.WHITE, 0.55f)
                    textSize = 50f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                    setShadowLayer(12f, 0f, 3f, ColorUtils.setAlphaComponent(deepColor, 160))
                }
                canvas.drawText(plant.scientificName!!, 60f, textBaseY, sciPaint)
                textBaseY += 72f
            }

            // ── 8. Accent divider ─────────────────────────────────────────────
            val divPaint = Paint().apply {
                color = ColorUtils.setAlphaComponent(accentColor, 180)
                strokeWidth = 3f
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
            }
            canvas.drawLine(60f, textBaseY + 16f, 200f, textBaseY + 16f, divPaint)
            textBaseY += 56f

            // ── 9. Insight quote ──────────────────────────────────────────────
            val insightPaint = Paint().apply {
                color = ColorUtils.blendARGB(Color.WHITE, Color.TRANSPARENT, 0.18f)
                textSize = 40f
                isAntiAlias = true
                setShadowLayer(10f, 0f, 2f, ColorUtils.setAlphaComponent(deepColor, 150))
            }
            // Limit insight to 2 lines / ~100 chars for elegance
            val rawInsight = plant.botanicalInsight.let {
                if (it.length > 110) it.take(107) + "…" else it
            }
            val insightLines = breakText(rawInsight, insightPaint, w - 140)
            val displayLines = insightLines.take(3)
            displayLines.forEachIndexed { i, line ->
                canvas.drawText(line, 60f, textBaseY + i * insightPaint.textSize * 1.5f, insightPaint)
            }
            textBaseY += displayLines.size * insightPaint.textSize * 1.5f + 32f

            // ── 10. Native region ─────────────────────────────────────────────
            val region = plant.nativeRegion?.takeIf { it.isNotBlank() }
            if (region != null && textBaseY < h - 260f) {
                val regionPaint = Paint().apply {
                    color = ColorUtils.setAlphaComponent(accentColor, 200)
                    textSize = 36f
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT_BOLD
                    letterSpacing = 0.04f
                }
                canvas.drawText("🌍 $region", 60f, textBaseY, regionPaint)
            }

            // ── 11. Bottom credit + CTA ────────────────────────────────────────
            val creditPaint = Paint().apply {
                color = ColorUtils.setAlphaComponent(Color.WHITE, 140)
                textSize = 34f
                isAntiAlias = true
            }
            canvas.drawText("📷 ${plant.photographerName} · Unsplash", 60f, h - 110f, creditPaint)

            val ctaPaint = Paint().apply {
                color = ColorUtils.setAlphaComponent(Color.WHITE, 200)
                textSize = 34f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.04f
            }
            canvas.drawText("🌿 Découvrez FloraFlow", 60f, h - 64f, ctaPaint)

            // ── 12. Save and share ────────────────────────────────────────────
            val dir = File(context.cacheDir, "story").also { it.mkdirs() }
            val file = File(dir, "floraflow_story.jpg")
            file.outputStream().use { story.compress(Bitmap.CompressFormat.JPEG, 94, it) }
            story.recycle()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "#FloraFlow #Botanique #${plant.plantName.replace(" ", "")}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Partager en story").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun breakText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            val test = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(test) > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = test
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }
}
