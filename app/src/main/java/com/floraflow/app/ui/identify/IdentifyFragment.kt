package com.floraflow.app.ui.identify

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
import com.bumptech.glide.Glide
import com.floraflow.app.databinding.FragmentIdentifyBinding
import java.io.File
import java.io.FileOutputStream

class IdentifyFragment : Fragment() {

    private var _binding: FragmentIdentifyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: IdentifyViewModel by viewModels()
    private var pendingImageFile: File? = null

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
        if (success) {
            pendingImageFile?.let { file ->
                viewModel.setImageUri(Uri.fromFile(file))
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            doLaunchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIdentifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.galleryButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        binding.cameraButton.setOnClickListener { requestCameraAndLaunch() }

        binding.identifyButton.setOnClickListener {
            val file = pendingImageFile ?: return@setOnClickListener
            viewModel.identify(file)
        }

        binding.resetButton.setOnClickListener { viewModel.reset() }

        viewModel.selectedImageUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                Glide.with(this).load(uri).centerCrop().into(binding.previewImage)
                binding.previewImage.visibility = View.VISIBLE
                binding.identifyButton.visibility = View.VISIBLE
                binding.placeholderGroup.visibility = View.GONE
            } else {
                binding.previewImage.visibility = View.GONE
                binding.identifyButton.visibility = View.GONE
                binding.placeholderGroup.visibility = View.VISIBLE
                binding.resultCard.visibility = View.GONE
                binding.errorText.visibility = View.GONE
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.identifyProgress.visibility = View.GONE
            when (state) {
                is IdentifyState.Idle -> {
                    binding.resultCard.visibility = View.GONE
                    binding.errorText.visibility = View.GONE
                    binding.identifyButton.isEnabled = true
                }
                is IdentifyState.Loading -> {
                    binding.identifyProgress.visibility = View.VISIBLE
                    binding.identifyButton.isEnabled = false
                    binding.resultCard.visibility = View.GONE
                    binding.errorText.visibility = View.GONE
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
                }
                is IdentifyState.Error -> {
                    binding.identifyButton.isEnabled = true
                    binding.resultCard.visibility = View.GONE
                    binding.errorText.visibility = View.VISIBLE
                    binding.errorText.text = state.message
                }
            }
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
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
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
