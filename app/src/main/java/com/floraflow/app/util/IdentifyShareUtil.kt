package com.floraflow.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object IdentifyShareUtil {

    fun share(
        context: Context,
        commonName: String,
        scientificName: String,
        confidence: Int,
        imageFile: File?
    ) {
        try {
            val w = 1080
            val h = 1080
            val card = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(card)

            // Background: plant photo or solid green
            if (imageFile != null && imageFile.exists()) {
                val raw = BitmapFactory.decodeFile(imageFile.absolutePath)
                if (raw != null) {
                    val scaled = Bitmap.createScaledBitmap(raw, w, h, true)
                    canvas.drawBitmap(scaled, 0f, 0f, null)
                    scaled.recycle()
                    raw.recycle()
                } else {
                    canvas.drawColor(Color.parseColor("#1B5E20"))
                }
            } else {
                canvas.drawColor(Color.parseColor("#1B5E20"))
            }

            // Dark gradient overlay (bottom 55%)
            val gradPaint = Paint()
            gradPaint.shader = LinearGradient(
                0f, h * 0.35f, 0f, h.toFloat(),
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.parseColor("#CC000000"),
                    Color.parseColor("#F0000000")
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, h * 0.35f, w.toFloat(), h.toFloat(), gradPaint)

            // Top branding pill
            val pillPaint = Paint().apply {
                color = Color.parseColor("#CC1B2E1F")
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(40f, 48f, 340f, 122f), 40f, 40f, pillPaint)
            val brandPaint = Paint().apply {
                color = Color.WHITE
                textSize = 42f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("🌿 FloraFlow", 72f, 100f, brandPaint)

            // "I just identified:" label
            val labelPaint = Paint().apply {
                color = Color.parseColor("#74C69D")
                textSize = 38f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.08f
            }
            canvas.drawText("I just identified:", 60f, h - 380f, labelPaint)

            // Plant name
            val namePaint = Paint().apply {
                color = Color.WHITE
                textSize = 88f
                isAntiAlias = true
                typeface = Typeface.SERIF
            }
            drawMultilineText(canvas, commonName, namePaint, 60f, h - 290f, w - 120, 100f)

            // Scientific name
            if (scientificName.isNotBlank()) {
                val sciPaint = Paint().apply {
                    color = Color.parseColor("#AAFFCC")
                    textSize = 44f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                }
                canvas.drawText(scientificName, 60f, h - 160f, sciPaint)
            }

            // Confidence badge
            val badgePaint = Paint().apply {
                color = Color.parseColor("#CC2D6A4F")
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(w - 240f, h - 126f, w - 40f, h - 60f), 30f, 30f, badgePaint)
            val confPaint = Paint().apply {
                color = Color.WHITE
                textSize = 34f
                isAntiAlias = true
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("$confidence% match", w - 220f, h - 80f, confPaint)

            // Bottom tagline
            val tagPaint = Paint().apply {
                color = Color.parseColor("#88FFFFFF")
                textSize = 30f
                isAntiAlias = true
            }
            canvas.drawText("Discover plants with FloraFlow", 60f, h - 30f, tagPaint)

            // Save and share
            val dir = File(context.cacheDir, "share")
            dir.mkdirs()
            val out = File(dir, "identified_plant.jpg")
            FileOutputStream(out).use { fos ->
                card.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            }
            card.recycle()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                out
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "I just identified a $commonName ($scientificName) with FloraFlow! 🌿"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share my discovery"))
        } catch (e: Exception) {
            // Fallback: text-only share
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "I just identified a $commonName ($scientificName) with FloraFlow! 🌿 $confidence% confidence."
                )
            }
            context.startActivity(Intent.createChooser(intent, "Share my discovery"))
        }
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        paint: Paint,
        x: Float,
        y: Float,
        maxWidth: Int,
        lineHeight: Float = paint.textSize * 1.2f
    ) {
        val words = text.split(" ")
        var line = ""
        var yPos = y
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line, x, yPos, paint)
                line = word
                yPos += lineHeight
            } else {
                line = test
            }
        }
        if (line.isNotEmpty()) canvas.drawText(line, x, yPos, paint)
    }
}
