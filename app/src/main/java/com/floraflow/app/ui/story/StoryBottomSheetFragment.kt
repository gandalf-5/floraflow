package com.floraflow.app.ui.story

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.floraflow.app.R
import com.floraflow.app.databinding.FragmentStoryBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class StoryBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentStoryBottomSheetBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StoryViewModel by viewModels()

    companion object {
        const val TAG = "StoryBottomSheet"
        private const val ARG_PLANT_NAME = "plant_name"
        private const val ARG_SCIENTIFIC_NAME = "scientific_name"
        private const val ARG_IS_PREMIUM = "is_premium"

        fun newInstance(
            plantName: String,
            scientificName: String?,
            isPremium: Boolean
        ) = StoryBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PLANT_NAME, plantName)
                putString(ARG_SCIENTIFIC_NAME, scientificName)
                putBoolean(ARG_IS_PREMIUM, isPremium)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoryBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val plantName = arguments?.getString(ARG_PLANT_NAME) ?: return
        val scientificName = arguments?.getString(ARG_SCIENTIFIC_NAME)
        val isPremium = arguments?.getBoolean(ARG_IS_PREMIUM, false) ?: false

        binding.storyPlantName.text = plantName
        if (!scientificName.isNullOrBlank()) {
            binding.storyScientificName.visibility = View.VISIBLE
            binding.storyScientificName.text = scientificName
        }

        if (!isPremium) {
            // Show sections as a faded teaser — don't hide them completely
            binding.historySection.alpha = 0.18f
            binding.folkloreSection.alpha = 0.18f
            binding.ecologySection.alpha = 0.18f
            binding.historySection.isClickable = false
            binding.folkloreSection.isClickable = false
            binding.ecologySection.isClickable = false
            binding.premiumLockCard.visibility = View.VISIBLE
        }

        binding.unlockPremiumButton.setOnClickListener {
            com.floraflow.app.ui.premium.PremiumBottomSheetFragment.newInstance()
                .show(parentFragmentManager, com.floraflow.app.ui.premium.PremiumBottomSheetFragment.TAG)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is StoryState.Loading -> {
                    binding.storyProgress.visibility = View.VISIBLE
                    binding.storyContent.visibility = View.GONE
                    binding.storyError.visibility = View.GONE
                }
                is StoryState.Success -> {
                    binding.storyProgress.visibility = View.GONE
                    binding.storyContent.visibility = View.VISIBLE
                    binding.storyError.visibility = View.GONE
                    // Always populate all sections — free users see them faded as preview
                    binding.etymologyText.text = state.story.etymology
                    binding.historyText.text = state.story.history
                    binding.folkloreText.text = state.story.folklore
                    binding.ecologyText.text = state.story.ecology
                }
                is StoryState.Error -> {
                    binding.storyProgress.visibility = View.GONE
                    binding.storyContent.visibility = View.GONE
                    binding.storyError.visibility = View.VISIBLE
                    binding.storyError.text = state.message
                }
                is StoryState.Idle -> {}
            }
        }

        if (viewModel.state.value is StoryState.Idle) {
            viewModel.loadStory(plantName, scientificName)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
