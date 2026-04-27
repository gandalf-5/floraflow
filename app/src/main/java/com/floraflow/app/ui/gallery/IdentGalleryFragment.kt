package com.floraflow.app.ui.gallery

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.text.StaticLayout
import android.text.TextPaint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.R
import com.floraflow.app.data.IdentificationRecord
import com.floraflow.app.data.PreferencesManager
import com.floraflow.app.databinding.FragmentIdentGalleryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IdentGalleryFragment : Fragment() {

    private var _binding: FragmentIdentGalleryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: IdentGalleryAdapter
    private val dateFmt = SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIdentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener { findNavController().navigateUp() }

        lifecycleScope.launch {
            val prefs = PreferencesManager(requireContext())
            val isPremium = prefs.isPremium.first()

            val app = requireActivity().application as FloraFlowApp
            val vm: IdentGalleryViewModel by viewModels {
                IdentGalleryViewModelFactory(app.database.identificationRecordDao(), isPremium)
            }

            adapter = IdentGalleryAdapter(
                onDelete = { record -> confirmDelete(app, record) },
                onSetWallpaper = { record ->
                    if (isPremium) applyWallpaper(record) else showUpgradeDialog()
                },
                onShare = { record -> shareRecord(record) }
            )

            binding.galleryRecycler.layoutManager = LinearLayoutManager(requireContext())
            binding.galleryRecycler.adapter = adapter

            if (!isPremium) binding.premiumBanner.visibility = View.VISIBLE

            binding.exportPdfButton.setOnClickListener {
                if (isPremium) exportPdf(adapter.currentList)
                else showUpgradeDialog(getString(R.string.gallery_export_premium_msg))
            }

            vm.records.observe(viewLifecycleOwner) { records ->
                adapter.submitList(records)
                binding.emptyGroup.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                binding.galleryRecycler.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
                binding.exportPdfButton.isEnabled = records.isNotEmpty()
            }
        }
    }

    // ── Share as social card ─────────────────────────────────────────────────

    private fun shareRecord(record: IdentificationRecord) {
        lifecycleScope.launch {
            try {
                binding.wallpaperProgress.visibility = View.VISIBLE
                val file = withContext(Dispatchers.IO) {
                    val bitmap = buildShareCard(record)
                    val dir = File(requireContext().cacheDir, "share").also { it.mkdirs() }
                    val out = File(dir, "floraflow_share.jpg")
                    FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                    bitmap.recycle()
                    out
                }
                binding.wallpaperProgress.visibility = View.GONE

                val uri = FileProvider.getUriForFile(
                    requireContext(), "${requireContext().packageName}.fileprovider", file
                )
                val confStr = getString(R.string.identify_confidence, record.confidence)
                val caption = buildString {
                    append("🌿 ${record.commonName} (${record.scientificName})\n")
                    append(confStr)
                    if (!record.locationName.isNullOrBlank()) append(" · 📍 ${record.locationName}")
                    append("\n\nIdentified with FloraFlow 🌱")
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share_plant)))
            } catch (e: Exception) {
                binding.wallpaperProgress.visibility = View.GONE
                Toast.makeText(requireContext(), getString(R.string.share_image_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildShareCard(record: IdentificationRecord): Bitmap {
        val W = 1080
        val H = 1350  // 4:5 — perfect for Instagram / Facebook

        val output = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. Background photo (scaled + center-cropped)
        val src = BitmapFactory.decodeFile(record.photoPath)
        if (src != null) {
            val srcRatio = src.width.toFloat() / src.height
            val dstRatio = W.toFloat() / H
            val (drawW, drawH) = if (srcRatio > dstRatio) {
                Pair((H * srcRatio).toInt(), H)
            } else {
                Pair(W, (W / srcRatio).toInt())
            }
            val scaled = Bitmap.createScaledBitmap(src, drawW, drawH, true)
            canvas.drawBitmap(scaled, -((drawW - W) / 2).toFloat(), -((drawH - H) / 2).toFloat(), null)
            if (scaled !== src) scaled.recycle()
            src.recycle()
        } else {
            canvas.drawColor(0xFF1A2A1A.toInt())
        }

        // 2. Bottom gradient overlay
        val gradPaint = Paint()
        gradPaint.shader = LinearGradient(
            0f, (H * 0.42f), 0f, H.toFloat(),
            intArrayOf(0x00000000, 0xEE000000.toInt()),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, H * 0.42f, W.toFloat(), H.toFloat(), gradPaint)

        // 3. Top bar gradient (for branding readability)
        val topGradPaint = Paint()
        topGradPaint.shader = LinearGradient(
            0f, 0f, 0f, 160f,
            intArrayOf(0x88000000.toInt(), 0x00000000),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, W.toFloat(), 160f, topGradPaint)

        // 4. FloraFlow branding (top left)
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xEEFFFFFF.toInt()
            textSize = 38f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("🌿 FloraFlow", 52f, 72f, brandPaint)

        // 5. Confidence badge (top right)
        val confBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC2D5016.toInt() }
        val confText = "${record.confidence}% match"
        val confTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFAAEE77.toInt()
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
        }
        val confW = confTextPaint.measureText(confText) + 40f
        canvas.drawRoundRect(W - confW - 48f, 44f, W - 48f, 92f, 24f, 24f, confBgPaint)
        canvas.drawText(confText, W - confW - 28f, 80f, confTextPaint)

        // 6. Plant name (big bold, wrapped)
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 88f
            typeface = Typeface.DEFAULT_BOLD
        }
        val nameLayout = StaticLayout.Builder
            .obtain(record.commonName, 0, record.commonName.length, namePaint, W - 100)
            .setMaxLines(2)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(52f, H - 310f - nameLayout.height)
        nameLayout.draw(canvas)
        canvas.restore()

        // 7. Scientific name
        val sciPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCCFFFFFF.toInt()
            textSize = 46f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        canvas.drawText(record.scientificName, 52f, H - 210f, sciPaint)

        // 8. Date + time
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xAAFFFFFF.toInt()
            textSize = 38f
            typeface = Typeface.DEFAULT
        }
        val dateStr = dateFmt.format(Date(record.timestampMs))
        canvas.drawText("🗓 $dateStr", 52f, H - 148f, metaPaint)

        // 9. Location
        if (!record.locationName.isNullOrBlank()) {
            canvas.drawText("📍 ${record.locationName}", 52f, H - 96f, metaPaint)
        }

        return output
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    private fun confirmDelete(app: FloraFlowApp, record: IdentificationRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.gallery_delete_title))
            .setMessage(getString(R.string.gallery_delete_message, record.commonName))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    app.database.identificationRecordDao().delete(record)
                    try { File(record.photoPath).delete() } catch (_: Exception) {}
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ── Wallpaper ────────────────────────────────────────────────────────────

    private fun applyWallpaper(record: IdentificationRecord) {
        lifecycleScope.launch {
            try {
                binding.wallpaperProgress.visibility = View.VISIBLE
                withContext(Dispatchers.IO) {
                    val bitmap = BitmapFactory.decodeFile(record.photoPath)
                        ?: throw IllegalStateException("Could not load photo")
                    WallpaperManager.getInstance(requireContext()).setBitmap(bitmap)
                    bitmap.recycle()
                }
                binding.wallpaperProgress.visibility = View.GONE
                Toast.makeText(requireContext(), getString(R.string.wallpaper_success), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                binding.wallpaperProgress.visibility = View.GONE
                Toast.makeText(requireContext(), getString(R.string.wallpaper_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── PDF Export ───────────────────────────────────────────────────────────

    private fun exportPdf(records: List<IdentificationRecord>) {
        if (records.isEmpty()) return
        lifecycleScope.launch {
            try {
                binding.wallpaperProgress.visibility = View.VISIBLE
                val file = withContext(Dispatchers.IO) { buildPdf(records) }
                binding.wallpaperProgress.visibility = View.GONE
                val uri = FileProvider.getUriForFile(
                    requireContext(), "${requireContext().packageName}.fileprovider", file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.journal_pdf_title))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.gallery_export_share_title)))
            } catch (e: Exception) {
                binding.wallpaperProgress.visibility = View.GONE
                Toast.makeText(requireContext(), getString(R.string.export_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildPdf(records: List<IdentificationRecord>): File {
        val PAGE_W = 595; val PAGE_H = 842; val MARGIN = 48
        val doc = PdfDocument()
        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val headPaint = Paint().apply { textSize = 15f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 12f }
        val subtlePaint = Paint().apply { textSize = 11f; alpha = 140 }
        val dividerPaint = Paint().apply { strokeWidth = 0.5f; alpha = 60 }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        var cvs: Canvas = page.canvas
        var y = MARGIN + 30

        fun finishAndNew() {
            doc.finishPage(page); pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            cvs = page.canvas; y = MARGIN + 20
        }
        fun space(n: Int) { if (y + n > PAGE_H - MARGIN) finishAndNew() }

        cvs.drawText("🌿 FloraFlow — Field Journal", MARGIN.toFloat(), y.toFloat(), titlePaint)
        y += 10
        cvs.drawLine(MARGIN.toFloat(), y.toFloat(), (PAGE_W - MARGIN).toFloat(), y.toFloat(), dividerPaint)
        y += 24

        for ((idx, r) in records.withIndex()) {
            space(90)
            cvs.drawText("${idx + 1}. ${r.commonName}", MARGIN.toFloat(), y.toFloat(), headPaint); y += 18
            cvs.drawText(r.scientificName, (MARGIN + 8).toFloat(), y.toFloat(), bodyPaint); y += 16
            val meta = "${r.confidence}% confidence" + if (!r.family.isNullOrBlank()) "  ·  Family: ${r.family}" else ""
            cvs.drawText(meta, (MARGIN + 8).toFloat(), y.toFloat(), subtlePaint); y += 15
            cvs.drawText(dateFmt.format(Date(r.timestampMs)), (MARGIN + 8).toFloat(), y.toFloat(), subtlePaint); y += 15
            if (!r.locationName.isNullOrBlank()) {
                cvs.drawText("📍 ${r.locationName}", (MARGIN + 8).toFloat(), y.toFloat(), subtlePaint); y += 15
            }
            y += 6
            cvs.drawLine(MARGIN.toFloat(), y.toFloat(), (PAGE_W - MARGIN).toFloat(), y.toFloat(), dividerPaint)
            y += 14
        }

        doc.finishPage(page)
        val file = File(requireContext().cacheDir, "floraflow_journal.pdf")
        doc.writeTo(FileOutputStream(file)); doc.close()
        return file
    }

    // ── Upgrade dialog ───────────────────────────────────────────────────────

    private fun showUpgradeDialog(message: String = "") {
        com.floraflow.app.ui.premium.PremiumBottomSheetFragment.newInstance()
            .show(parentFragmentManager, com.floraflow.app.ui.premium.PremiumBottomSheetFragment.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
