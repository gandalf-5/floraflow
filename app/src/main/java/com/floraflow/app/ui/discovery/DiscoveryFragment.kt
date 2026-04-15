package com.floraflow.app.ui.discovery

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.R
import com.floraflow.app.data.DailyPlant
import com.floraflow.app.data.PlantRepository
import com.floraflow.app.databinding.FragmentDiscoveryBinding

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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        binding.refreshFab.setOnClickListener { viewModel.refresh() }
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
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.wallpaper_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.resetWallpaperState()
                }
                is WallpaperState.Error -> {
                    binding.setWallpaperButton.isEnabled = true
                    binding.setWallpaperButton.text = getString(R.string.set_as_wallpaper)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.wallpaper_error),
                        Toast.LENGTH_SHORT
                    ).show()
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
        binding.contentGroup.visibility = View.VISIBLE

        binding.plantNameText.text = plant.plantName

        if (!plant.locationName.isNullOrBlank()) {
            binding.locationGroup.visibility = View.VISIBLE
            binding.locationText.text = plant.locationName
        } else {
            binding.locationGroup.visibility = View.GONE
        }

        binding.botanicalInsightText.text = plant.botanicalInsight

        binding.photographerText.text = getString(
            R.string.photo_credit,
            plant.photographerName
        )
        binding.photographerText.setOnClickListener {
            openUrl(plant.photographerProfileUrl)
        }

        binding.setWallpaperButton.setOnClickListener {
            viewModel.setAsWallpaper(requireContext(), plant)
        }

        Glide.with(this)
            .load(plant.imageUrlRegular)
            .transition(DrawableTransitionOptions.withCrossFade(600))
            .placeholder(R.drawable.placeholder_plant)
            .into(binding.plantImage)
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
