package com.floraflow.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.floraflow.app.R
import com.floraflow.app.data.PreferencesManager
import com.floraflow.app.databinding.FragmentSettingsBinding
import com.floraflow.app.worker.WallpaperScheduler
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: PreferencesManager
    private var isPremium = false

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
            prefs.isPremium.collect { premium -> isPremium = premium }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            prefs.autoSyncWallpaper.collect { enabled ->
                binding.autoSyncToggle.isChecked = enabled
                val v = if (enabled) View.VISIBLE else View.GONE
                binding.wallpaperTimeRow.visibility = v
                binding.wallpaperTargetRow.visibility = v
                binding.wallpaperIntervalRow.visibility = v
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            prefs.wallpaperIntervalMinutes.collect { minutes ->
                val label = intervalLabel(minutes)
                if (isPremium) {
                    binding.wallpaperIntervalValue.text = label
                    binding.wallpaperIntervalValue.visibility = View.VISIBLE
                    binding.wallpaperIntervalPremiumBadge.visibility = View.GONE
                } else {
                    binding.wallpaperIntervalPremiumBadge.visibility = View.VISIBLE
                    binding.wallpaperIntervalValue.visibility = View.GONE
                }
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

        viewLifecycleOwner.lifecycleScope.launch {
            prefs.darkMode.collect { mode ->
                binding.darkModeValue.text = when (mode) {
                    PreferencesManager.DARK_MODE_ON -> getString(R.string.dark_mode_dark)
                    PreferencesManager.DARK_MODE_OFF -> getString(R.string.dark_mode_light)
                    else -> getString(R.string.dark_mode_system)
                }
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
        binding.wallpaperIntervalRow.setOnClickListener {
            if (isPremium) showIntervalPicker() else showIntervalUpgradeDialog()
        }
        binding.categoriesRow.setOnClickListener { showCategoriesPicker() }
        binding.darkModeRow.setOnClickListener { showDarkModePicker() }
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
                viewLifecycleOwner.lifecycleScope.launch { prefs.setWallpaperTarget(which) }
            }
            .show()
    }

    private fun showCategoriesPicker() {
        val all = PreferencesManager.ALL_CATEGORIES
        val displayNames = PreferencesManager.DISPLAY_CATEGORIES.toTypedArray()
        val checked = BooleanArray(all.size) { true }

        viewLifecycleOwner.lifecycleScope.launch {
            prefs.preferredCategories.collect { selected ->
                all.forEachIndexed { i, cat -> checked[i] = cat in selected }
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.preferred_categories))
            .setMultiChoiceItems(displayNames, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val selected = all.filterIndexed { i, _ -> checked[i] }
                val final = if (selected.isEmpty()) all else selected
                viewLifecycleOwner.lifecycleScope.launch { prefs.setPreferredCategories(final) }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDarkModePicker() {
        val options = arrayOf(
            getString(R.string.dark_mode_system),
            getString(R.string.dark_mode_light),
            getString(R.string.dark_mode_dark)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dark_mode))
            .setItems(options) { _, which ->
                viewLifecycleOwner.lifecycleScope.launch {
                    prefs.setDarkMode(which)
                    AppCompatDelegate.setDefaultNightMode(
                        when (which) {
                            PreferencesManager.DARK_MODE_OFF -> AppCompatDelegate.MODE_NIGHT_NO
                            PreferencesManager.DARK_MODE_ON -> AppCompatDelegate.MODE_NIGHT_YES
                            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }
                    )
                }
            }
            .show()
    }

    private fun showIntervalPicker() {
        val options = arrayOf(
            getString(R.string.interval_3h),
            getString(R.string.interval_6h),
            getString(R.string.interval_12h),
            getString(R.string.interval_24h)
        )
        val minutes = intArrayOf(
            PreferencesManager.INTERVAL_3H,
            PreferencesManager.INTERVAL_6H,
            PreferencesManager.INTERVAL_12H,
            PreferencesManager.INTERVAL_24H
        )
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.wallpaper_interval))
            .setItems(options) { _, which ->
                viewLifecycleOwner.lifecycleScope.launch {
                    prefs.setWallpaperIntervalMinutes(minutes[which])
                    WallpaperScheduler.schedule(requireContext())
                }
            }
            .show()
    }

    private fun showIntervalUpgradeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.story_premium_title))
            .setMessage(getString(R.string.wallpaper_interval_premium_msg))
            .setPositiveButton(getString(R.string.story_unlock_button), null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun intervalLabel(minutes: Int): String = when (minutes) {
        PreferencesManager.INTERVAL_3H  -> getString(R.string.interval_3h)
        PreferencesManager.INTERVAL_6H  -> getString(R.string.interval_6h)
        PreferencesManager.INTERVAL_12H -> getString(R.string.interval_12h)
        else                            -> getString(R.string.interval_24h)
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
