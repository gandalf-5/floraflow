package com.floraflow.app.ui.discovery

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.R
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.data.PreferencesManager
import com.floraflow.app.databinding.FragmentDiscoveryBinding
import com.floraflow.app.ui.discovery.DiscoveryViewModelFactory
import com.google.android.material.chip.Chip
import java.io.File

class DiscoveryFragment : Fragment() {

    private var _binding: FragmentDiscoveryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiscoveryViewModel by viewModels {
        val app = requireActivity().application as FloraFlowApp
        DiscoveryViewModelFactory(
            PlantRepository(app.database.dailyPlantDao(), app.unsplashApi, app.openAiApi)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoryChips()
        setupObservers()
        binding.refreshFab.setOnClickListener {
            binding.categoryChipGroup.clearCheck()
            viewModel.refresh()
        }
    }

    private fun setupCategoryChips() {
        val queries = PreferencesManager.ALL_CATEGORIES
        val labels = PreferencesManager.DISPLAY_CATEGORIES

        queries.forEachIndexed { index, query ->
            val chip = Chip(requireContext()).apply {
                text = labels.getOrElse(index) { query }
                isCheckable = true
                chipBackgroundColor = resources.getColorStateList(
                    com.google.android.material.R.color.m3_chip_background_color, requireContext().theme
                )
                setTextColor(resources.getColor(R.color.text_secondary, requireContext().theme))
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) viewModel.refreshWithCategory(query)
                }
            }
            binding.categoryChipGroup.addView(chip)
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DiscoveryUiState.Loading -> showLoading()
                is DiscoveryUiState.Success -> showPlant(state.plant)
                is DiscoveryUiState.Error -> showError(state.message)
            }
        }

        viewModel.wallpaperState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is WallpaperState.Idle -> {
                    binding.setWallpaperButton.isEnabled = true
                    binding.setWallpaperButton.text = getString(R.string.set_as_wallpaper)
                }
                is WallpaperState.Setting -> {
                    binding.setWallpaperButton.isEnabled = false
                    binding.setWallpaperButton.text = getString(R.string.wallpaper_setting)
                }
                is WallpaperState.Success -> {
                    binding.setWallpaperButton.isEnabled = true
                    binding.setWallpaperButton.text = getString(R.string.set_as_wallpaper)
                    Toast.makeText(requireContext(), getString(R.string.wallpaper_success), Toast.LENGTH_SHORT).show()
                    viewModel.resetWallpaperState()
                }
                is WallpaperState.Error -> {
                    binding.setWallpaperButton.isEnabled = true
                    binding.setWallpaperButton.text = getString(R.string.set_as_wallpaper)
                    Toast.makeText(requireContext(), getString(R.string.wallpaper_error), Toast.LENGTH_SHORT).show()
                    viewModel.resetWallpaperState()
                }
            }
        }
    }

    private fun showLoading() {
        binding.loadingGroup.visibility = View.VISIBLE
        binding.contentGroup.visibility = View.GONE
        binding.errorGroup.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.loadingGroup.visibility = View.GONE
        binding.contentGroup.visibility = View.GONE
        binding.errorGroup.visibility = View.VISIBLE
        binding.errorText.text = message
        binding.retryButton.setOnClickListener { viewModel.refresh() }
    }

    private fun showPlant(plant: DailyPlant) {
        binding.loadingGroup.visibility = View.GONE
        binding.errorGroup.visibility = View.GONE

        if (binding.contentGroup.visibility != View.VISIBLE) {
            binding.contentGroup.visibility = View.VISIBLE
            val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.content_fade_in)
            binding.contentGroup.startAnimation(fadeIn)
        }

        binding.plantNameText.text = plant.plantName

        if (!plant.scientificName.isNullOrBlank()) {
            binding.scientificNameText.visibility = View.VISIBLE
            binding.scientificNameText.text = plant.scientificName
        } else {
            binding.scientificNameText.visibility = View.GONE
        }

        if (!plant.locationName.isNullOrBlank()) {
            binding.locationGroup.visibility = View.VISIBLE
            binding.locationText.text = plant.locationName
        } else {
            binding.locationGroup.visibility = View.GONE
        }

        binding.botanicalInsightText.text = plant.botanicalInsight
        binding.photographerText.text = getString(R.string.photo_credit, plant.photographerName)
        binding.photographerText.setOnClickListener { openUrl(plant.photographerProfileUrl) }

        val favIcon = if (plant.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
        binding.favoriteButton.setImageResource(favIcon)
        binding.favoriteButton.setOnClickListener { viewModel.toggleFavorite(plant) }
        binding.shareButton.setOnClickListener { sharePlant(plant) }
        binding.setWallpaperButton.setOnClickListener { viewModel.setAsWallpaper(requireContext(), plant) }

        Glide.with(this)
            .load(plant.imageUrlRegular)
            .transition(DrawableTransitionOptions.withCrossFade(600))
            .placeholder(R.drawable.placeholder_plant)
            .into(binding.plantImage)
    }

    private fun sharePlant(plant: DailyPlant) {
        val shareText = buildString {
            append("🌿 ${plant.plantName}")
            if (!plant.scientificName.isNullOrBlank()) append(" (${plant.scientificName})")
            append("\n\n")
            append(plant.botanicalInsight)
            append("\n\nPhoto by ${plant.photographerName} on Unsplash")
            append("\n\nDiscover more with FloraFlow 🌱")
        }

        val ctx = requireContext()
        val imageDir = File(ctx.cacheDir, "images").also { it.mkdirs() }
        val imageFile = File(imageDir, "share_plant.jpg")

        Thread {
            try {
                val bitmap = Glide.with(ctx).asBitmap().load(plant.imageUrlRegular).submit().get()
                imageFile.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", imageFile)
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.share_plant)))
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.share_plant)))
                }
            }
        }.start()
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
