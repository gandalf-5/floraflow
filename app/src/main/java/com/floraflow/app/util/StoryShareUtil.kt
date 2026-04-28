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
import com.floraflow.app.data.DailyPlant
import java.io.File
import kotlin.math.max
import kotlin.math.min

object StoryShareUtil {

    /**
     * Samples a 16×16 thumbnail of the bitmap to extract the most saturated / vivid
     * dominant color — no external Palette library required.
     */
    private fun extractAccentColor(bitmap: Bitmap): Int {
        val thumb = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        val pixels = IntArray(16 * 16)
        thumb.getPixels(pixels, 0, 16, 0, 0, 16, 16)
        thumb.recycle()

        var bestScore = -1f
        var bestColor = Color.parseColor("#2D6A4F")

        for (pixel in pixels) {
            val r = Color.red(pixel) / 255f
            val g = Color.green(pixel) / 255f
            val b = Color.blue(pixel) / 255f
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val saturation = if (maxC > 0) (maxC - minC) / maxC else 0f
            val brightness = maxC
            val score = saturation * brightness
            if (score > bestScore) {
                bestScore = score
                bestColor = pixel
            }
        }
        return bestColor
    }

    fun share(context: Context, plant: DailyPlant, plantBitmap: Bitmap) {
        try {
            val w = 1080
            val h = 1920

            val accentColor = extractAccentColor(plantBitmap)
            val deepColor = ColorUtils.blendARGB(accentColor, Color.BLACK, 0.72f)

            val story = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(story)

            // ── 1. Background: center-cropped plant photo ─────────────────────
            val srcRatio = plantBitmap.width.toFloat() / plantBitmap.height.toFloat()
            val dstRatio = w.toFloat() / h.toFloat()
            val (srcLeft, srcTop, srcW, srcH) = if (srcRatio > dstRatio) {
                val nW = (plantBitmap.height * dstRatio).toInt()
                listOf((plantBitmap.width - nW) / 2, 0, nW, plantBitmap.height)
            } else {
                val nH = (plantBitmap.width / dstRatio).toInt()
                listOf(0, (plantBitmap.height - nH) / 2, plantBitmap.width, nH)
            }
            canvas.drawBitmap(plantBitmap,
                android.graphics.Rect(srcLeft, srcTop, srcLeft + srcW, srcTop + srcH),
                android.graphics.Rect(0, 0, w, h), null)

            // ── 2. Gradient overlay ───────────────────────────────────────────
            val gradPaint = Paint()
            gradPaint.shader = LinearGradient(
                0f, h * 0.28f, 0f, h.toFloat(),
                intArrayOf(Color.TRANSPARENT,
                    ColorUtils.setAlphaComponent(deepColor, 170),
                    ColorUtils.setAlphaComponent(deepColor, 230),
                    deepColor),
                floatArrayOf(0f, 0.35f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, h * 0.28f, w.toFloat(), h.toFloat(), gradPaint)

            // ── 3. Decorative rings (top-right corner) ────────────────────────
            val ringPaint = Paint().apply {
                color = ColorUtils.setAlphaComponent(accentColor, 50)
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            val cx = w + 80f; val cy = -80f
            canvas.drawCircle(cx, cy, 340f, ringPaint)
            canvas.drawCircle(cx, cy, 255f, ringPaint)
            canvas.drawCircle(cx, cy, 170f, ringPaint)
            val dotPaint = Paint().apply {
                color = ColorUtils.setAlphaComponent(accentColor, 80)
                isAntiAlias = true
            }
            canvas.drawCircle(cx, cy, 90f, dotPaint)

            // ── 4. Top branding pill ──────────────────────────────────────────
            val pillBg = Paint().apply {
                color = ColorUtils.setAlphaComponent(deepColor, 195)
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(48f, 58f, 370f, 134f), 40f, 40f, pillBg)
            val brandPaint = Paint().apply {
                color = Color.WHITE
                textSize = 44f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.06f
                setShadowLayer(6f, 0f, 2f, ColorUtils.setAlphaComponent(deepColor, 160))
            }
            canvas.drawText("🌿 FloraFlow", 78f, 110f, brandPaint)

            // ── 5. Location badge ─────────────────────────────────────────────
            var textY = h * 0.685f
            val location = plant.locationName?.takeIf { it.isNotBlank() }
            if (location != null) {
                val locBg = Paint().apply { color = ColorUtils.setAlphaComponent(Color.WHITE, 215); isAntiAlias = true }
                val locText = Paint().apply {
                    color = deepColor; textSize = 35f; isAntiAlias = true
                    typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.08f
                }
                val label = "📍 ${location.uppercase()}"
                val lw = locText.measureText(label)
                canvas.drawRoundRect(RectF(60f, textY - 50f, 60f + lw + 40f, textY + 8f), 28f, 28f, locBg)
                canvas.drawText(label, 80f, textY - 12f, locText)
                textY += 74f
            }

            // ── 6. Plant name ─────────────────────────────────────────────────
            val namePaint = Paint().apply {
                color = Color.WHITE
                textSize = if (plant.plantName.length > 14) 86f else 102f
                isAntiAlias = true
                typeface = Typeface.SERIF
                setShadowLayer(16f, 0f, 4f, ColorUtils.setAlphaComponent(deepColor, 195))
            }
            val nameLines = breakText(plant.plantName, namePaint, w - 120)
            nameLines.forEachIndexed { i, line ->
                canvas.drawText(line, 60f, textY + i * namePaint.textSize * 1.25f, namePaint)
            }
            textY += nameLines.size * namePaint.textSize * 1.25f + 20f

            // ── 7. Scientific name ────────────────────────────────────────────
            if (!plant.scientificName.isNullOrBlank()) {
                val sciPaint = Paint().apply {
                    color = ColorUtils.blendARGB(accentColor, Color.WHITE, 0.6f)
                    textSize = 48f; isAntiAlias = true
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                    setShadowLayer(10f, 0f, 3f, ColorUtils.setAlphaComponent(deepColor, 150))
                }
                canvas.drawText(plant.scientificName!!, 60f, textY, sciPaint)
                textY += 70f
            }

            // ── 8. Accent divider ─────────────────────────────────────────────
            val divPaint = Paint().apply {
                color = ColorUtils.setAlphaComponent(accentColor, 175)
                strokeWidth = 3f; strokeCap = Paint.Cap.ROUND; isAntiAlias = true
            }
            canvas.drawLine(60f, textY + 14f, 195f, textY + 14f, divPaint)
            textY += 52f

            // ── 9. Botanical insight (2–3 lines) ──────────────────────────────
            val insightPaint = Paint().apply {
                color = ColorUtils.setAlphaComponent(Color.WHITE, 218)
                textSize = 40f; isAntiAlias = true
                setShadowLayer(8f, 0f, 2f, ColorUtils.setAlphaComponent(deepColor, 140))
            }
            val raw = plant.botanicalInsight.let { if (it.length > 115) it.take(112) + "…" else it }
            val insightLines = breakText(raw, insightPaint, w - 140).take(3)
            insightLines.forEachIndexed { i, line ->
                canvas.drawText(line, 60f, textY + i * insightPaint.textSize * 1.5f, insightPaint)
            }
            textY += insightLines.size * insightPaint.textSize * 1.5f + 28f

            // ── 10. Native region tag ─────────────────────────────────────────
            val region = plant.nativeRegion?.takeIf { it.isNotBlank() }
            if (region != null && textY < h - 280f) {
                val regionPaint = Paint().apply {
                    color = ColorUtils.blendARGB(accentColor, Color.WHITE, 0.45f)
                    textSize = 35f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText("🌍 $region", 60f, textY, regionPaint)
            }

            // ── 11. Credits + CTA ─────────────────────────────────────────────
            val creditPaint = Paint().apply {
                color = ColorUtils.setAlphaComponent(Color.WHITE, 135)
                textSize = 33f; isAntiAlias = true
            }
            canvas.drawText("📷 ${plant.photographerName} · Unsplash", 60f, h - 110f, creditPaint)
            val ctaPaint = Paint().apply {
                color = ColorUtils.setAlphaComponent(Color.WHITE, 200)
                textSize = 33f; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.04f
            }
            canvas.drawText("🌿 Découvrez FloraFlow", 60f, h - 62f, ctaPaint)

            // ── 12. Save & share ──────────────────────────────────────────────
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
            context.startActivity(Intent.createChooser(intent, "Partager en story").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun breakText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var cur = ""
        for (word in words) {
            val test = if (cur.isEmpty()) word else "$cur $word"
            if (paint.measureText(test) > maxWidth && cur.isNotEmpty()) {
                lines.add(cur); cur = word
            } else { cur = test }
        }
        if (cur.isNotEmpty()) lines.add(cur)
        return lines
    }
}
