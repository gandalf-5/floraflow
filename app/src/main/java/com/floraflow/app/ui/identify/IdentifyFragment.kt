package com.floraflow.app.ui.identify

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.floraflow.app.R
import com.floraflow.app.data.PreferencesManager
import com.floraflow.app.databinding.FragmentIdentifyBinding
import com.floraflow.app.ui.story.StoryBottomSheetFragment
import com.floraflow.app.util.IdentifyShareUtil
import com.floraflow.app.util.RatingManager
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class IdentifyFragment : Fragment() {

    private var _binding: FragmentIdentifyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: IdentifyViewModel by viewModels()
    private var pendingImageFile: File? = null
    private var lastKnownLocation: Location? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            viewModel.setImageUri(uri)
            copyUriToFile(uri)
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) pendingImageFile?.let { viewModel.setImageUri(Uri.fromFile(it)) }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) doLaunchCamera()
        else Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) refreshLastLocation() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIdentifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestLocationPermissionIfNeeded()

        binding.galleryButton.setOnClickListener {
            pickImageLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
        }
        binding.cameraButton.setOnClickListener { requestCameraAndLaunch() }
        binding.myIdentificationsButton.setOnClickListener {
            findNavController().navigate(R.id.action_identify_to_gallery)
        }
        binding.identifyButton.setOnClickListener {
            val file = pendingImageFile ?: return@setOnClickListener
            val loc = lastKnownLocation
            viewModel.identify(file, loc?.latitude, loc?.longitude)
        }
        binding.resetButton.setOnClickListener { viewModel.reset() }

        // Counter: show "X/2 today" for free users
        viewModel.isPremiumUser.observe(viewLifecycleOwner) { isPremium ->
            if (isPremium) {
                binding.dailyCounterText.visibility = View.GONE
            }
        }
        viewModel.dailyUsed.observe(viewLifecycleOwner) { used ->
            val isPremium = viewModel.isPremiumUser.value ?: false
            if (!isPremium && used > 0) {
                val limit = PreferencesManager.FREE_DAILY_ID_LIMIT
                binding.dailyCounterText.visibility = View.VISIBLE
                binding.dailyCounterText.text = getString(R.string.identify_daily_counter, used, limit)
                val atLimit = used >= limit
                binding.dailyCounterText.alpha = if (atLimit) 1f else 0.75f
                binding.identifyButton.isEnabled = !atLimit
                if (atLimit) {
                    binding.dailyCounterText.setTextColor(
                        resources.getColor(android.R.color.holo_red_light, null)
                    )
                } else {
                    binding.dailyCounterText.setTextColor(
                        resources.getColor(R.color.primary, null)
                    )
                }
            } else if (!isPremium) {
                binding.dailyCounterText.visibility = View.GONE
            }
        }

        viewModel.selectedImageUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                Glide.with(this).load(uri).centerCrop().into(binding.previewImage)
                binding.previewImage.visibility = View.VISIBLE
                val atLimit = (viewModel.dailyUsed.value ?: 0) >= PreferencesManager.FREE_DAILY_ID_LIMIT
                        && viewModel.isPremiumUser.value != true
                binding.identifyButton.visibility = View.VISIBLE
                binding.identifyButton.isEnabled = !atLimit
                binding.placeholderGroup.visibility = View.GONE
            } else {
                binding.previewImage.visibility = View.GONE
                binding.identifyButton.visibility = View.GONE
                binding.placeholderGroup.visibility = View.VISIBLE
                binding.resultCard.visibility = View.GONE
                binding.errorText.visibility = View.GONE
            }
        }

        viewModel.careTipsLoading.observe(viewLifecycleOwner) { loading ->
            binding.careTipsProgress.visibility = if (loading) View.VISIBLE else View.GONE
            if (loading) binding.careTipsCard.visibility = View.VISIBLE
        }

        viewModel.careTips.observe(viewLifecycleOwner) { tips ->
            if (tips == null) {
                if (viewModel.careTipsLoading.value != true) {
                    binding.careTipsCard.visibility = View.GONE
                }
                return@observe
            }
            binding.careTipsCard.visibility = View.VISIBLE
            binding.careTipsProgress.visibility = View.GONE
            binding.careWatering.text    = tips.watering
            binding.careLight.text       = tips.light
            binding.careSoil.text        = tips.soil
            binding.careTemperature.text = tips.temperature
            binding.careToxicity.text    = tips.toxicity
            binding.careSeasonalTip.text = tips.seasonalTip
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.identifyProgress.visibility = View.GONE
            when (state) {
                is IdentifyState.Idle -> {
                    binding.resultCard.visibility = View.GONE
                    binding.errorText.visibility = View.GONE
                    binding.identifyButton.isEnabled = true
                    binding.storyButton.visibility = View.GONE
                    binding.shareResultButton.visibility = View.GONE
                    binding.careTipsCard.visibility = View.GONE
                }
                is IdentifyState.Loading -> {
                    binding.identifyProgress.visibility = View.VISIBLE
                    binding.identifyButton.isEnabled = false
                    binding.resultCard.visibility = View.GONE
                    binding.errorText.visibility = View.GONE
                    binding.shareResultButton.visibility = View.GONE
                }
                is IdentifyState.LimitReached -> {
                    binding.identifyButton.isEnabled = false
                    binding.shareResultButton.visibility = View.GONE
                    showLimitReachedDialog(state.limit)
                }
                is IdentifyState.Result -> {
                    binding.identifyButton.isEnabled = true
                    binding.resultCard.visibility = View.VISIBLE
                    binding.errorText.visibility = View.GONE
                    binding.resultCommonName.text = state.commonName
                    binding.resultScientificName.text = state.scientificName
                    binding.resultConfidence.text = "${state.confidence}% match"
                    binding.resultFamily.text = state.family ?: ""
                    binding.resultFamily.visibility =
                        if (state.family != null) View.VISIBLE else View.GONE
                    binding.resetButton.visibility = View.VISIBLE
                    binding.storyButton.visibility = View.VISIBLE
                    binding.shareResultButton.visibility = View.VISIBLE
                    binding.shareResultButton.setOnClickListener {
                        IdentifyShareUtil.share(
                            requireContext(),
                            state.commonName,
                            state.scientificName,
                            state.confidence,
                            pendingImageFile
                        )
                    }
                    binding.storyButton.setOnClickListener {
                        val prefs = PreferencesManager(requireContext())
                        lifecycleScope.launch {
                            val isPremium = prefs.isPremium.first()
                            StoryBottomSheetFragment.newInstance(
                                plantName = state.commonName,
                                scientificName = state.scientificName,
                                isPremium = isPremium
                            ).show(parentFragmentManager, StoryBottomSheetFragment.TAG)
                        }
                    }
                    // Rating: record positive event, ask after 3rd identification
                    RatingManager.recordPositiveEvent(requireContext())
                    activity?.let { RatingManager.requestReviewIfAppropriate(it) }
                }
                is IdentifyState.Error -> {
                    binding.identifyButton.isEnabled = true
                    binding.resultCard.visibility = View.GONE
                    binding.errorText.visibility = View.VISIBLE
                    binding.errorText.text = state.message
                    binding.shareResultButton.visibility = View.GONE
                }
            }
        }
    }

    private fun showLimitReachedDialog(limit: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.identify_limit_title))
            .setMessage(getString(R.string.identify_limit_message, limit))
            .setPositiveButton(getString(R.string.story_unlock_button), null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun requestLocationPermissionIfNeeded() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> refreshLastLocation()
            else -> locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @Suppress("MissingPermission")
    private fun refreshLastLocation() {
        try {
            val lm = requireContext().getSystemService(LocationManager::class.java)
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            lastKnownLocation = providers.mapNotNull { lm.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
        } catch (e: Exception) {
            lastKnownLocation = null
        }
    }

    private fun requestCameraAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> doLaunchCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun doLaunchCamera() {
        val file = File(requireContext().cacheDir, "identify_${System.currentTimeMillis()}.jpg")
        pendingImageFile = file
        val uri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        takePictureLauncher.launch(uri)
    }

    private fun copyUriToFile(uri: Uri) {
        val file = File(requireContext().cacheDir, "identify_${System.currentTimeMillis()}.jpg")
        pendingImageFile = file
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
