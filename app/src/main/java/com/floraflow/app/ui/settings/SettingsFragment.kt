package com.floraflow.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.floraflow.app.FloraFlowApp
import com.floraflow.app.data.PreferencesManager
import com.floraflow.app.databinding.FragmentSettingsBinding
import com.floraflow.app.worker.WallpaperScheduler
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as FloraFlowApp
        preferencesManager = PreferencesManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            preferencesManager.autoSyncWallpaper.collect { enabled ->
                binding.autoSyncToggle.isChecked = enabled
            }
        }

        binding.autoSyncToggle.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                preferencesManager.setAutoSyncWallpaper(isChecked)
                if (isChecked) {
                    WallpaperScheduler.schedule(requireContext())
                } else {
                    WallpaperScheduler.cancel(requireContext())
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
