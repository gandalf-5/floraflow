package com.floraflow.app.ui.gallery

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
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
                }
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

    private fun applyWallpaper(record: IdentificationRecord) {
        lifecycleScope.launch {
            try {
                binding.wallpaperProgress.visibility = View.VISIBLE
                withContext(Dispatchers.IO) {
                    val bitmap = BitmapFactory.decodeFile(record.photoPath)
                        ?: throw IllegalStateException("Could not load photo")
                    val wm = WallpaperManager.getInstance(requireContext())
                    wm.setBitmap(bitmap)
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
                    putExtra(Intent.EXTRA_SUBJECT, "FloraFlow Field Journal")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.gallery_export_share_title)))
            } catch (e: Exception) {
                binding.wallpaperProgress.visibility = View.GONE
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun buildPdf(records: List<IdentificationRecord>): File {
        val PAGE_W = 595
        val PAGE_H = 842
        val MARGIN = 48
        val doc = PdfDocument()
        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val headPaint = Paint().apply { textSize = 15f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 12f }
        val subtlePaint = Paint().apply { textSize = 11f; alpha = 140 }
        val dividerPaint = Paint().apply { strokeWidth = 0.5f; alpha = 60 }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        var canvas: Canvas = page.canvas
        var y = MARGIN + 30

        fun finishAndNewPage() {
            doc.finishPage(page)
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            canvas = page.canvas
            y = MARGIN + 20
        }

        fun ensureSpace(needed: Int) { if (y + needed > PAGE_H - MARGIN) finishAndNewPage() }

        // Header
        canvas.drawText("🌿 FloraFlow — Field Journal", MARGIN.toFloat(), y.toFloat(), titlePaint)
        y += 10
        canvas.drawLine(MARGIN.toFloat(), y.toFloat(), (PAGE_W - MARGIN).toFloat(), y.toFloat(), dividerPaint)
        y += 24

        for ((idx, record) in records.withIndex()) {
            ensureSpace(90)

            // Entry number + plant name
            headPaint.isFakeBoldText = true
            canvas.drawText("${idx + 1}. ${record.commonName}", MARGIN.toFloat(), y.toFloat(), headPaint)
            y += 18

            // Scientific name
            bodyPaint.textSize = 12f
            canvas.drawText(record.scientificName, (MARGIN + 8).toFloat(), y.toFloat(), bodyPaint)
            y += 16

            // Confidence + family
            val meta = buildString {
                append("${record.confidence}% confidence")
                if (!record.family.isNullOrBlank()) append("  ·  Family: ${record.family}")
            }
            subtlePaint.textSize = 11f
            canvas.drawText(meta, (MARGIN + 8).toFloat(), y.toFloat(), subtlePaint)
            y += 15

            // Date
            canvas.drawText(dateFmt.format(Date(record.timestampMs)), (MARGIN + 8).toFloat(), y.toFloat(), subtlePaint)
            y += 15

            // Location
            if (!record.locationName.isNullOrBlank()) {
                canvas.drawText("📍 ${record.locationName}", (MARGIN + 8).toFloat(), y.toFloat(), subtlePaint)
                y += 15
            }

            // Divider
            y += 6
            canvas.drawLine(MARGIN.toFloat(), y.toFloat(), (PAGE_W - MARGIN).toFloat(), y.toFloat(), dividerPaint)
            y += 14
        }

        doc.finishPage(page)

        val file = File(requireContext().cacheDir, "floraflow_journal.pdf")
        doc.writeTo(FileOutputStream(file))
        doc.close()
        return file
    }

    private fun showUpgradeDialog(message: String = getString(R.string.gallery_wallpaper_premium_msg)) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.story_premium_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.story_unlock_button), null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
