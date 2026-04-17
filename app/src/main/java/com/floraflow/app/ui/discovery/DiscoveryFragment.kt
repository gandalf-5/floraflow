package com.floraflow.app.ui.discovery

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import androidx.lifecycle.lifecycleScope
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.R
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.data.PreferencesManager
import com.floraflow.app.databinding.FragmentDiscoveryBinding
import com.floraflow.app.ui.story.StoryBottomSheetFragment
import com.floraflow.app.util.StoryShareUtil
import com.google.android.material.chip.Chip
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DiscoveryFragment : Fragment() {

    private var _binding: FragmentDiscoveryBinding? = null
    private val binding get() = _binding!!

    private var currentBitmap: Bitmap? = null
    private var currentPlant: DailyPlant? = null

    private val viewModel: DiscoveryViewModel by viewModels {
        val app = requireActivity().application as FloraFlowApp
        DiscoveryViewModelFactory(
            PlantRepository(app.database.dailyPlantDao(), app.unsplashApi, app.floraFlowApi),
            PreferencesManager(requireContext())
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
        setupSwipeGesture()
        setupObservers()

        binding.refreshFab.setOnClickListener {
            binding.categoryChipGroup.clearCheck()
            viewModel.refresh()
        }
        binding.quizCard.setOnClickListener {
            findNavController().navigate(R.id.action_discovery_to_quiz)
        }
        binding.seasonsCard.setOnClickListener {
            findNavController().navigate(R.id.action_discovery_to_seasonal)
        }
    }

    private fun setupCategoryChips() {
        val queries = PreferencesManager.ALL_CATEGORIES
        val labels = PreferencesManager.DISPLAY_CATEGORIES
        queries.forEachIndexed { index, query ->
            val chip = Chip(requireContext()).apply {
                text = labels.getOrElse(index) { query }
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) viewModel.refreshWithCategory(query)
                }
            }
            binding.categoryChipGroup.addView(chip)
        }
    }

    private fun setupSwipeGesture() {
        val categories = PreferencesManager.ALL_CATEGORIES
        var categoryIndex = 0
        val gestureDetector = GestureDetector(requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                private val SWIPE_THRESHOLD = 100
                private val SWIPE_VELOCITY_THRESHOLD = 100

                override fun onFling(
                    e1: MotionEvent?, e2: MotionEvent,
                    velocityX: Float, velocityY: Float
                ): Boolean {
                    val e1x = e1?.x ?: return false
                    val diffX = e2.x - e1x
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX < 0) {
                            categoryIndex = (categoryIndex + 1) % categories.size
                        } else {
                            categoryIndex = (categoryIndex - 1 + categories.size) % categories.size
                        }
                        binding.categoryChipGroup.clearCheck()
                        viewModel.refreshWithCategory(categories[categoryIndex])
                        binding.swipeHint.visibility = View.GONE
                        return true
                    }
                    return false
                }
            })

        binding.plantImage.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            v.performClick()
            true
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
                    Toast.makeText(requireContext(), R.string.wallpaper_success, Toast.LENGTH_SHORT).show()
                    viewModel.resetWallpaperState()
                }
                is WallpaperState.Error -> {
                    binding.setWallpaperButton.isEnabled = true
                    binding.setWallpaperButton.text = getString(R.string.set_as_wallpaper)
                    Toast.makeText(requireContext(), R.string.wallpaper_error, Toast.LENGTH_SHORT).show()
                    viewModel.resetWallpaperState()
                }
            }
        }

        viewModel.streakCount.observe(viewLifecycleOwner) { streak ->
            if (streak > 0) {
                binding.streakBadge.visibility = View.VISIBLE
                binding.streakText.text = "$streak days"
            } else {
                binding.streakBadge.visibility = View.GONE
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
        currentPlant = plant
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

        if (!plant.nativeRegion.isNullOrBlank()) {
            binding.nativeRegionGroup.visibility = View.VISIBLE
            binding.nativeRegionText.text = "Native to ${plant.nativeRegion}"
        } else {
            binding.nativeRegionGroup.visibility = View.GONE
        }

        if (!plant.locationName.isNullOrBlank()) {
            binding.locationGroup.visibility = View.VISIBLE
            binding.locationText.text = plant.locationName
        } else {
            binding.locationGroup.visibility = View.GONE
        }

        if (!plant.notes.isNullOrBlank()) {
            binding.notesPreview.visibility = View.VISIBLE
            binding.notesPreview.text = "📝 ${plant.notes}"
        } else {
            binding.notesPreview.visibility = View.GONE
        }

        binding.botanicalInsightText.text = plant.botanicalInsight
        binding.photographerText.text = getString(R.string.photo_credit, plant.photographerName)
        binding.photographerText.setOnClickListener { openUrl(plant.photographerProfileUrl) }

        val favIcon = if (plant.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
        binding.favoriteButton.setImageResource(favIcon)
        binding.favoriteButton.setOnClickListener { viewModel.toggleFavorite(plant) }

        binding.notesButton.setOnClickListener { showNotesDialog(plant) }

        binding.shareButton.setOnClickListener { sharePlant(plant) }

        binding.storyShareButton.setOnClickListener {
            val bmp = currentBitmap
            if (bmp != null) {
                StoryShareUtil.share(requireContext(), plant, bmp)
            } else {
                Toast.makeText(requireContext(), getString(R.string.loading_plant), Toast.LENGTH_SHORT).show()
            }
        }

        binding.setWallpaperButton.setOnClickListener { viewModel.setAsWallpaper(requireContext(), plant) }

        binding.storyButton.setOnClickListener {
            val prefs = PreferencesManager(requireContext())
            lifecycleScope.launch {
                val isPremium = prefs.isPremium.first()
                val sheet = StoryBottomSheetFragment.newInstance(
                    plantName = plant.plantName,
                    scientificName = plant.scientificName,
                    isPremium = isPremium
                )
                sheet.show(parentFragmentManager, StoryBottomSheetFragment.TAG)
            }
        }

        loadImageWithPalette(plant)
    }

    private fun loadImageWithPalette(plant: DailyPlant) {
        Glide.with(this)
            .asBitmap()
            .load(plant.imageUrlRegular)
            .placeholder(R.drawable.placeholder_plant)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (!isAdded) return
                    currentBitmap = resource
                    binding.plantImage.setImageBitmap(resource)
                    extractAndShowPalette(resource)
                }
                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                    binding.plantImage.setImageDrawable(placeholder)
                }
            })
    }

    private fun extractAndShowPalette(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            if (!isAdded || palette == null) return@generate
            val swatches = listOfNotNull(
                palette.vibrantSwatch,
                palette.lightVibrantSwatch,
                palette.mutedSwatch,
                palette.darkVibrantSwatch,
                palette.darkMutedSwatch
            ).take(5)

            if (swatches.isEmpty()) return@generate

            binding.paletteRow.removeAllViews()
            binding.paletteRow.visibility = View.VISIBLE

            swatches.forEach { swatch ->
                val dot = View(requireContext()).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(36, 36).apply {
                        marginEnd = 8
                    }
                    setBackgroundColor(swatch.rgb)
                    background = android.graphics.drawable.GradientDrawable().also {
                        it.shape = android.graphics.drawable.GradientDrawable.OVAL
                        it.setColor(swatch.rgb)
                    }
                }
                binding.paletteRow.addView(dot)
            }
        }
    }

    private fun showNotesDialog(plant: DailyPlant) {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.notes_hint)
            setText(plant.notes ?: "")
            maxLines = 6
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.notes_title))
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.saveNotes(plant, editText.text.toString())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(getString(R.string.notes_clear)) { _, _ ->
                viewModel.saveNotes(plant, null)
            }
            .show()
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
        val bmp = currentBitmap
        if (bmp != null) {
            try {
                val dir = File(ctx.cacheDir, "images").also { it.mkdirs() }
                val file = File(dir, "share_plant.jpg")
                file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share_plant)))
                return
            } catch (e: Exception) { /* fall through to text share */ }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_plant)))
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) { }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
