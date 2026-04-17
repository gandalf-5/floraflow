package com.floraflow.app.ui.gallery

import android.app.AlertDialog
import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

class IdentGalleryFragment : Fragment() {

    private var _binding: FragmentIdentGalleryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: IdentGalleryAdapter

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
                    if (isPremium) {
                        applyWallpaper(record)
                    } else {
                        showUpgradeDialog()
                    }
                }
            )

            binding.galleryRecycler.layoutManager = LinearLayoutManager(requireContext())
            binding.galleryRecycler.adapter = adapter

            if (!isPremium) {
                binding.premiumBanner.visibility = View.VISIBLE
            }

            vm.records.observe(viewLifecycleOwner) { records ->
                adapter.submitList(records)
                binding.emptyGroup.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                binding.galleryRecycler.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
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

    private fun showUpgradeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.story_premium_title))
            .setMessage(getString(R.string.gallery_wallpaper_premium_msg))
            .setPositiveButton(getString(R.string.story_unlock_button), null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
