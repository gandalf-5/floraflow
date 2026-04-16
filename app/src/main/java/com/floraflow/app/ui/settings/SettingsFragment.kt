package com.floraflow.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.floraflow.app.R
import com.floraflow.app.data.PreferencesManager
import com.floraflow.app.databinding.FragmentSettingsBinding
import com.floraflow.app.worker.WallpaperScheduler
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferencesManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            prefs.autoSyncWallpaper.collect { enabled ->
                binding.autoSyncToggle.isChecked = enabled
                binding.wallpaperTimeRow.visibility = if (enabled) View.VISIBLE else View.GONE
                binding.wallpaperTargetRow.visibility = if (enabled) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            prefs.wallpaperHour.collect { hour ->
                binding.wallpaperTimeValue.text = formatHour(hour)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            prefs.wallpaperTarget.collect { target ->
                binding.wallpaperTargetValue.text = when (target) {
                    PreferencesManager.TARGET_HOME -> getString(R.string.target_home)
                    PreferencesManager.TARGET_LOCK -> getString(R.string.target_lock)
                    else -> getString(R.string.target_all)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            prefs.preferredCategories.collect { categories ->
                binding.categoriesValue.text = if (categories.size == PreferencesManager.ALL_CATEGORIES.size) {
                    getString(R.string.all_categories)
                } else {
                    "${categories.size} ${getString(R.string.categories_selected)}"
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            prefs.notificationsEnabled.collect { enabled ->
                binding.notificationsToggle.isChecked = enabled
            }
        }

        binding.autoSyncToggle.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                prefs.setAutoSyncWallpaper(isChecked)
                if (isChecked) WallpaperScheduler.schedule(requireContext())
                else WallpaperScheduler.cancel(requireContext())
            }
        }

        binding.notificationsToggle.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                prefs.setNotificationsEnabled(isChecked)
            }
        }

        binding.wallpaperTimeRow.setOnClickListener { showTimePicker() }
        binding.wallpaperTargetRow.setOnClickListener { showTargetPicker() }
        binding.categoriesRow.setOnClickListener { showCategoriesPicker() }
    }

    private fun showTimePicker() {
        val picker = NumberPicker(requireContext()).apply {
            minValue = 0
            maxValue = 23
            displayedValues = Array(24) { h -> formatHour(h) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            prefs.wallpaperHour.collect { h -> picker.value = h }
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.wallpaper_time))
            .setView(picker)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    prefs.setWallpaperHour(picker.value)
                    WallpaperScheduler.schedule(requireContext())
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showTargetPicker() {
        val options = arrayOf(
            getString(R.string.target_all),
            getString(R.string.target_home),
            getString(R.string.target_lock)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.wallpaper_target))
            .setItems(options) { _, which ->
                viewLifecycleOwner.lifecycleScope.launch {
                    prefs.setWallpaperTarget(which)
                }
            }
            .show()
    }

    private fun showCategoriesPicker() {
        val all = PreferencesManager.ALL_CATEGORIES
        val displayNames = all.map { it.replaceFirstChar { c -> c.titlecase() } }.toTypedArray()
        val checked = BooleanArray(all.size) { true }

        viewLifecycleOwner.lifecycleScope.launch {
            prefs.preferredCategories.collect { selected ->
                all.forEachIndexed { i, cat -> checked[i] = cat in selected }
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.preferred_categories))
            .setMultiChoiceItems(displayNames, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val selected = all.filterIndexed { i, _ -> checked[i] }
                val final = if (selected.isEmpty()) all else selected
                viewLifecycleOwner.lifecycleScope.launch {
                    prefs.setPreferredCategories(final)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun formatHour(hour: Int): String {
        return when {
            hour == 0 -> "12:00 AM"
            hour < 12 -> "${hour}:00 AM"
            hour == 12 -> "12:00 PM"
            else -> "${hour - 12}:00 PM"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
